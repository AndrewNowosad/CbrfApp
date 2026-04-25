package ru.cbrf.rates.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.cbrf.rates.data.local.prefs.AppPreferences
import ru.cbrf.rates.data.local.prefs.AppTheme
import ru.cbrf.rates.data.local.prefs.UpdateInterval
import ru.cbrf.rates.worker.RateUpdateWorker
import javax.inject.Inject

data class SettingsUiState(
    val theme: AppTheme = AppTheme.AUTO,
    val updateInterval: UpdateInterval = UpdateInterval.H1,
    val decimalPlaces: Int = 4,
    val invertColors: Boolean = false,
    val widgetBgAlpha: Float = 0.85f,
    val widgetCornerRadius: Float = 16f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(prefs.theme, prefs.updateInterval, prefs.decimalPlaces) { a, b, c -> Triple(a, b, c) },
        combine(prefs.invertColors, prefs.widgetBgAlpha, prefs.widgetCornerRadius) { a, b, c -> Triple(a, b, c) }
    ) { (theme, interval, decimals), (invert, alpha, radius) ->
        SettingsUiState(
            theme = theme,
            updateInterval = interval,
            decimalPlaces = decimals,
            invertColors = invert,
            widgetBgAlpha = alpha,
            widgetCornerRadius = radius
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTheme(theme: AppTheme) = viewModelScope.launch { prefs.setTheme(theme) }

    fun setUpdateInterval(interval: UpdateInterval) = viewModelScope.launch {
        prefs.setUpdateInterval(interval)
        RateUpdateWorker.schedule(context, interval)
    }

    fun setDecimalPlaces(places: Int) = viewModelScope.launch { prefs.setDecimalPlaces(places) }

    fun setInvertColors(invert: Boolean) = viewModelScope.launch { prefs.setInvertColors(invert) }

    fun setWidgetBgAlpha(alpha: Float) = viewModelScope.launch { prefs.setWidgetBgAlpha(alpha) }

    fun setWidgetCornerRadius(radius: Float) = viewModelScope.launch { prefs.setWidgetCornerRadius(radius) }
}
