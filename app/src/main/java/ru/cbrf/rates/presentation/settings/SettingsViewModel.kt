package ru.cbrf.rates.presentation.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.cbrf.rates.data.local.prefs.AppLanguage
import ru.cbrf.rates.data.local.prefs.AppPreferences
import ru.cbrf.rates.data.local.prefs.AppTheme
import ru.cbrf.rates.data.local.prefs.UpdateInterval
import ru.cbrf.rates.data.local.prefs.WidgetBgColorMode
import ru.cbrf.rates.widget.WidgetUpdateHelper
import ru.cbrf.rates.worker.RateUpdateWorker
import javax.inject.Inject

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.AUTO,
    val theme: AppTheme = AppTheme.AUTO,
    val updateInterval: UpdateInterval = UpdateInterval.H1,
    val decimalPlaces: Int = 4,
    val invertColors: Boolean = false,
    val widgetBgAlpha: Float = 0.85f,
    val widgetCornerRadius: Float = 16f,
    val widgetBgColorMode: WidgetBgColorMode = WidgetBgColorMode.AUTO
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "Unhandled coroutine exception", e)
    }

    private data class WidgetPrefs(
        val invertColors: Boolean,
        val widgetBgAlpha: Float,
        val widgetCornerRadius: Float,
        val widgetBgColorMode: WidgetBgColorMode
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(prefs.language, prefs.theme, prefs.updateInterval, prefs.decimalPlaces) { lang, theme, interval, decimals ->
            SettingsUiState(language = lang, theme = theme, updateInterval = interval, decimalPlaces = decimals)
        },
        combine(prefs.invertColors, prefs.widgetBgAlpha, prefs.widgetCornerRadius, prefs.widgetBgColorMode) { invert, alpha, radius, colorMode ->
            WidgetPrefs(invert, alpha, radius, colorMode)
        }
    ) { base, widgetPrefs ->
        base.copy(
            invertColors = widgetPrefs.invertColors,
            widgetBgAlpha = widgetPrefs.widgetBgAlpha,
            widgetCornerRadius = widgetPrefs.widgetCornerRadius,
            widgetBgColorMode = widgetPrefs.widgetBgColorMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setLanguage(language: AppLanguage) = viewModelScope.launch(exceptionHandler) { prefs.setLanguage(language) }
    fun setTheme(theme: AppTheme) = viewModelScope.launch(exceptionHandler) { prefs.setTheme(theme) }

    fun setUpdateInterval(interval: UpdateInterval) = viewModelScope.launch(exceptionHandler) {
        prefs.setUpdateInterval(interval)
        RateUpdateWorker.schedule(context, interval)
    }

    fun setDecimalPlaces(places: Int) = viewModelScope.launch(exceptionHandler) {
        prefs.setDecimalPlaces(places)
        updateAllWidgets()
    }

    fun setInvertColors(invert: Boolean) = viewModelScope.launch(exceptionHandler) {
        prefs.setInvertColors(invert)
        updateAllWidgets()
    }

    fun setWidgetBgAlpha(alpha: Float) = viewModelScope.launch(exceptionHandler) {
        prefs.setWidgetBgAlpha(alpha)
        updateAllWidgets()
    }

    fun setWidgetCornerRadius(radius: Float) = viewModelScope.launch(exceptionHandler) {
        prefs.setWidgetCornerRadius(radius)
        updateAllWidgets()
    }

    fun setWidgetBgColorMode(mode: WidgetBgColorMode) = viewModelScope.launch(exceptionHandler) {
        prefs.setWidgetBgColorMode(mode)
        updateAllWidgets()
    }

    fun updateWidgetsNow() = viewModelScope.launch(exceptionHandler) {
        updateAllWidgets()
    }

    private suspend fun updateAllWidgets() {
        WidgetUpdateHelper.requestUpdate(context)
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
