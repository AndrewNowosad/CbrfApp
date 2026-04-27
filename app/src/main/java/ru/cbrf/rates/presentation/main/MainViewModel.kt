package ru.cbrf.rates.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.cbrf.rates.data.local.prefs.AppPreferences
import ru.cbrf.rates.domain.model.CurrencyRateUiModel
import ru.cbrf.rates.domain.repository.RateRepository
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
    private val repository: RateRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "Unhandled coroutine exception", e)
    }

    private val _displayDate = MutableStateFlow(LocalDate.now())
    private val _isLoading = MutableStateFlow(false)
    private val _hasError = MutableStateFlow(false)
    private val _rates = MutableStateFlow<List<CurrencyRateUiModel>>(emptyList())
    private val _effectiveDate = MutableStateFlow(LocalDate.now())

    private data class DateRates(
        val displayDate: LocalDate,
        val effectiveDate: LocalDate,
        val rates: List<CurrencyRateUiModel>
    )

    private data class Prefs(
        val isLoading: Boolean,
        val hasError: Boolean,
        val decimalPlaces: Int,
        val invertColors: Boolean
    )

    val uiState: StateFlow<MainUiState> = combine(
        combine(_displayDate, _effectiveDate, _rates) { displayDate, effectiveDate, rates ->
            DateRates(displayDate, effectiveDate, rates)
        },
        combine(_isLoading, _hasError, appPreferences.decimalPlaces, appPreferences.invertColors) { isLoading, hasError, decimalPlaces, invertColors ->
            Prefs(isLoading, hasError, decimalPlaces, invertColors)
        }
    ) { dateRates, prefs ->
        MainUiState(
            displayDate = dateRates.displayDate,
            effectiveDate = dateRates.effectiveDate,
            rates = dateRates.rates,
            isLoading = prefs.isLoading,
            hasError = prefs.hasError,
            decimalPlaces = prefs.decimalPlaces,
            invertColors = prefs.invertColors,
            hasTomorrow = dateRates.rates.any { it.tomorrowValue != null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch(exceptionHandler) {
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
        val clamped = minOf(date, LocalDate.now().plusDays(1))
        _displayDate.value = clamped
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            _hasError.value = false
            repository.fetchRatesIfNeeded(clamped).onFailure { _hasError.value = true }
            val displayEffective = repository.getLatestAvailableDate(clamped) ?: clamped
            loadRatesForDate(displayEffective)
            _isLoading.value = false
        }
    }

    private suspend fun loadRatesForDate(date: LocalDate) {
        _rates.value = getRatesForDisplay(date)
    }

    fun jumpToToday() {
        _displayDate.value = LocalDate.now()
        refresh(force = false)
    }

    fun jumpToTomorrow() {
        val tomorrow = LocalDate.now().plusDays(1)
        setDisplayDate(tomorrow)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
