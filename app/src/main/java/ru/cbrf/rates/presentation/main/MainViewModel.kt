package ru.cbrf.rates.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.cbrf.rates.data.local.prefs.AppPreferences
import ru.cbrf.rates.domain.model.CurrencyRateUiModel
import ru.cbrf.rates.domain.usecase.GetRatesForDisplayUseCase
import ru.cbrf.rates.domain.usecase.RefreshTodayRatesUseCase
import java.time.LocalDate
import javax.inject.Inject

data class MainUiState(
    val displayDate: LocalDate = LocalDate.now(),
    val effectiveDate: LocalDate = LocalDate.now(),
    val rates: List<CurrencyRateUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val decimalPlaces: Int = 4,
    val invertColors: Boolean = false,
    val hasTomorrow: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getRatesForDisplay: GetRatesForDisplayUseCase,
    private val refreshTodayRates: RefreshTodayRatesUseCase,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _displayDate = MutableStateFlow(LocalDate.now())
    private val _isLoading = MutableStateFlow(false)
    private val _hasError = MutableStateFlow(false)
    private val _rates = MutableStateFlow<List<CurrencyRateUiModel>>(emptyList())
    private val _effectiveDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<MainUiState> = combine(
        combine(_displayDate, _effectiveDate, _rates) { a, b, c -> Triple(a, b, c) },
        combine(_isLoading, _hasError, appPreferences.decimalPlaces, appPreferences.invertColors) { a, b, c, d ->
            listOf(a, b, c, d)
        }
    ) { (displayDate, effectiveDate, rates), extras ->
        @Suppress("UNCHECKED_CAST")
        val ratesList = rates as List<CurrencyRateUiModel>
        val isLoading = extras[0] as Boolean
        val hasError = extras[1] as Boolean
        val decimals = extras[2] as Int
        val invert = extras[3] as Boolean
        MainUiState(
            displayDate = displayDate,
            effectiveDate = effectiveDate,
            rates = ratesList,
            isLoading = isLoading,
            hasError = hasError,
            decimalPlaces = decimals,
            invertColors = invert,
            hasTomorrow = ratesList.any { it.tomorrowValue != null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _hasError.value = false
            val result = refreshTodayRates(force = force)
            result.onSuccess { effectiveDate ->
                _effectiveDate.value = effectiveDate
                if (_displayDate.value == LocalDate.now()) {
                    loadRatesForDate(effectiveDate)
                } else {
                    loadRatesForDate(_displayDate.value)
                }
            }.onFailure {
                _hasError.value = true
                loadRatesForDate(_displayDate.value)
            }
            _isLoading.value = false
        }
    }

    fun setDisplayDate(date: LocalDate) {
        _displayDate.value = date
        viewModelScope.launch {
            _isLoading.value = true
            loadRatesForDate(date)
            _isLoading.value = false
        }
    }

    private suspend fun loadRatesForDate(date: LocalDate) {
        _rates.value = getRatesForDisplay(date)
    }

    fun jumpToToday() {
        setDisplayDate(_effectiveDate.value)
    }

    fun jumpToTomorrow() {
        val tomorrow = LocalDate.now().plusDays(1)
        setDisplayDate(tomorrow)
    }
}
