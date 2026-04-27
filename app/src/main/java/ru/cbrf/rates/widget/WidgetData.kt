package ru.cbrf.rates.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.cbrf.rates.data.local.prefs.WidgetBgColorMode
import ru.cbrf.rates.domain.model.CurrencyRateUiModel

data class WidgetDisplayData(
    val appWidgetId: Int,
    val currencies: List<CurrencyRateUiModel>,
    val displayDate: String,
    val decimalPlaces: Int,
    val invertColors: Boolean,
    val bgAlpha: Float,
    val cornerRadius: Float,
    val bgColorMode: WidgetBgColorMode
)

internal data class PrefsSnapshot(
    val currencies: List<String>,
    val decimalPlaces: Int,
    val invertColors: Boolean,
    val bgAlpha: Float,
    val cornerRadius: Float,
    val bgColorMode: WidgetBgColorMode
)

internal object WidgetStateKeys {
    val APP_WIDGET_ID = intPreferencesKey("wid")
    val DISPLAY_DATE = stringPreferencesKey("date")
    val DECIMAL_PLACES = intPreferencesKey("decimals")
    val INVERT_COLORS = booleanPreferencesKey("invert")
    val BG_ALPHA = floatPreferencesKey("bgAlpha")
    val CORNER_RADIUS = floatPreferencesKey("cornerRadius")
    val BG_COLOR_MODE = stringPreferencesKey("bgColorMode")
    val CURRENCIES_DATA = stringPreferencesKey("currencies")
}

internal fun MutablePreferences.writeWidgetData(data: WidgetDisplayData) {
    this[WidgetStateKeys.APP_WIDGET_ID] = data.appWidgetId
    this[WidgetStateKeys.DISPLAY_DATE] = data.displayDate
    this[WidgetStateKeys.DECIMAL_PLACES] = data.decimalPlaces
    this[WidgetStateKeys.INVERT_COLORS] = data.invertColors
    this[WidgetStateKeys.BG_ALPHA] = data.bgAlpha
    this[WidgetStateKeys.CORNER_RADIUS] = data.cornerRadius
    this[WidgetStateKeys.BG_COLOR_MODE] = data.bgColorMode.name
    this[WidgetStateKeys.CURRENCIES_DATA] = data.currencies.encodeCurrencies()
}

internal fun Preferences.readWidgetData(): WidgetDisplayData? {
    val appWidgetId = this[WidgetStateKeys.APP_WIDGET_ID] ?: return null
    return WidgetDisplayData(
        appWidgetId = appWidgetId,
        currencies = this[WidgetStateKeys.CURRENCIES_DATA].decodeCurrencies(),
        displayDate = this[WidgetStateKeys.DISPLAY_DATE] ?: "",
        decimalPlaces = this[WidgetStateKeys.DECIMAL_PLACES] ?: 4,
        invertColors = this[WidgetStateKeys.INVERT_COLORS] ?: false,
        bgAlpha = this[WidgetStateKeys.BG_ALPHA] ?: 0.85f,
        cornerRadius = this[WidgetStateKeys.CORNER_RADIUS] ?: 16f,
        bgColorMode = runCatching {
            WidgetBgColorMode.valueOf(this[WidgetStateKeys.BG_COLOR_MODE] ?: "AUTO")
        }.getOrDefault(WidgetBgColorMode.AUTO)
    )
}

private fun List<CurrencyRateUiModel>.encodeCurrencies(): String =
    Json.encodeToString(this)

private fun String?.decodeCurrencies(): List<CurrencyRateUiModel> {
    if (isNullOrBlank()) return emptyList()
    return runCatching { Json.decodeFromString<List<CurrencyRateUiModel>>(this) }
        .getOrDefault(emptyList())
}
