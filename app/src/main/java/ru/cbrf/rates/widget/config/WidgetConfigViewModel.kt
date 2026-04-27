package ru.cbrf.rates.widget.config

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.cbrf.rates.data.local.prefs.WidgetPreferences
import ru.cbrf.rates.domain.model.CurrencyMeta
import ru.cbrf.rates.domain.usecase.GetRatesForDisplayUseCase
import ru.cbrf.rates.domain.usecase.RefreshTodayRatesUseCase
import ru.cbrf.rates.widget.WidgetUpdateHelper
import java.time.LocalDate
import javax.inject.Inject

data class CurrencyItem(
    val charCode: String,
    val nameRu: String,
    val nameEn: String,
    val flagEmoji: String,
    val isSelected: Boolean
) {
    val displayName: String get() = if (java.util.Locale.getDefault().language == "ru") nameRu else nameEn
}

data class WidgetConfigUiState(
    val items: List<CurrencyItem> = emptyList(),
    val selectedCodes: List<String> = emptyList(),
    val maxCurrencies: Int = 1,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    private val widgetPrefs: WidgetPreferences,
    private val getRates: GetRatesForDisplayUseCase,
    private val refreshRates: RefreshTodayRatesUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(WidgetConfigUiState())
    val state: StateFlow<WidgetConfigUiState> = _state.asStateFlow()

    private var appWidgetId: Int = -1

    fun init(appWidgetId: Int, sizeHint: String? = null) {
        if (this.appWidgetId == appWidgetId) return
        this.appWidgetId = appWidgetId
        Log.d("CbrfWidget", "init appWidgetId=$appWidgetId sizeHint=$sizeHint")

        viewModelScope.launch {
            val savedCurrencies = widgetPrefs.getCurrenciesOnce(appWidgetId)
            val sizeName = widgetPrefs.getSize(appWidgetId) ?: sizeHint
            if (sizeHint != null && widgetPrefs.getSize(appWidgetId) == null) {
                widgetPrefs.setSize(appWidgetId, sizeHint)
            }
            val maxCurrencies = when (sizeName) {
                "MEDIUM" -> 2
                "LARGE" -> 4
                else -> 1
            }

            // Use effective date (handles weekends/holidays — may differ from today)
            val effectiveDate = refreshRates(force = false).getOrNull() ?: LocalDate.now()
            val allRates = getRates(effectiveDate)
            val allItems = allRates.map { rate ->
                CurrencyItem(
                    charCode = rate.charCode,
                    nameRu = rate.nameRu,
                    nameEn = rate.nameEn,
                    flagEmoji = rate.flagEmoji,
                    isSelected = rate.charCode in savedCurrencies
                )
            }.sortedWith(compareByDescending<CurrencyItem> { it.isSelected }.thenBy { it.charCode })

            _state.value = WidgetConfigUiState(
                items = allItems,
                selectedCodes = savedCurrencies.toMutableList(),
                maxCurrencies = maxCurrencies,
                isLoading = false
            )
        }
    }

    fun setSize(sizeName: String) {
        viewModelScope.launch { widgetPrefs.setSize(appWidgetId, sizeName) }
        val max = when (sizeName) {
            "MEDIUM" -> 2
            "LARGE" -> 4
            else -> 1
        }
        val trimmed = _state.value.selectedCodes.take(max)
        _state.value = _state.value.copy(maxCurrencies = max, selectedCodes = trimmed)
        updateItemsSelection(trimmed)
    }

    fun toggleCurrency(charCode: String) {
        val current = _state.value.selectedCodes.toMutableList()
        if (charCode in current) {
            current.remove(charCode)
        } else if (current.size < _state.value.maxCurrencies) {
            current.add(charCode)
        }
        _state.value = _state.value.copy(selectedCodes = current)
        updateItemsSelection(current)
    }

    fun moveUp(charCode: String) {
        val current = _state.value.selectedCodes.toMutableList()
        val idx = current.indexOf(charCode)
        if (idx > 0) {
            current.removeAt(idx)
            current.add(idx - 1, charCode)
            _state.value = _state.value.copy(selectedCodes = current)
        }
    }

    fun moveDown(charCode: String) {
        val current = _state.value.selectedCodes.toMutableList()
        val idx = current.indexOf(charCode)
        if (idx < current.size - 1) {
            current.removeAt(idx)
            current.add(idx + 1, charCode)
            _state.value = _state.value.copy(selectedCodes = current)
        }
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun save(onDone: suspend () -> Unit) {
        val codes = _state.value.selectedCodes
        Log.d("CbrfWidget", "save CALLED appWidgetId=$appWidgetId codes=$codes")
        viewModelScope.launch {
            widgetPrefs.setCurrencies(appWidgetId, codes)
            Log.d("CbrfWidget", "save setCurrencies DONE appWidgetId=$appWidgetId codes=$codes")
            WidgetUpdateHelper.requestUpdate(context)
            Log.d("CbrfWidget", "save requestUpdate DONE, calling onDone")
            onDone()
        }
    }

    private fun updateItemsSelection(selected: List<String>) {
        _state.value = _state.value.copy(
            items = _state.value.items.map { it.copy(isSelected = it.charCode in selected) }
        )
    }
}
