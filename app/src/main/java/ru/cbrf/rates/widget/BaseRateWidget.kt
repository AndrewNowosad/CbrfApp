package ru.cbrf.rates.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.async
import ru.cbrf.rates.domain.repository.RateRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import ru.cbrf.rates.domain.model.CurrencyRateUiModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

abstract class BaseRateWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    internal suspend fun loadData(context: Context, glanceId: GlanceId, maxCurrencies: Int): WidgetDisplayData {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val widgetPrefs = ep.widgetPreferences()
        val appPrefs = ep.appPreferences()
        val getRates = ep.getRatesForDisplay()

        val repository = ep.rateRepository()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val today = LocalDate.now()
        Log.d("CbrfWidget", "loadData START appWidgetId=$appWidgetId glanceId=$glanceId")

        val (currencies, decimalPlaces, invertColors, bgAlpha, cornerRadius) = coroutineScope {
            val c = async { widgetPrefs.getCurrenciesOnce(appWidgetId).take(maxCurrencies) }
            val d = async { appPrefs.decimalPlaces.first() }
            val i = async { appPrefs.invertColors.first() }
            val a = async { appPrefs.widgetBgAlpha.first() }
            val r = async { appPrefs.widgetCornerRadius.first() }
            PrefsSnapshot(c.await(), d.await(), i.await(), a.await(), r.await())
        }
        Log.d("CbrfWidget", "loadData currencies=$currencies bgAlpha=$bgAlpha")

        // Find effective date; if DB is empty, fetch from network first
        var effectiveDate = repository.getLatestAvailableDate(today) ?: run {
            ep.refreshTodayRates()(force = false)
            repository.getLatestAvailableDate(today) ?: today
        }

        val rates = if (currencies.isEmpty()) {
            emptyList()
        } else {
            var result = getRates(effectiveDate, today)
                .filter { it.charCode in currencies }
                .sortedBy { currencies.indexOf(it.charCode) }

            if (result.isEmpty()) {
                // Data missing for selected currencies — fetch and retry
                ep.refreshTodayRates()(force = false)
                effectiveDate = repository.getLatestAvailableDate(today) ?: today
                result = getRates(effectiveDate, today)
                    .filter { it.charCode in currencies }
                    .sortedBy { currencies.indexOf(it.charCode) }
            }
            result
        }

        val dateStr = effectiveDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        Log.d("CbrfWidget", "loadData END effectiveDate=$effectiveDate rates=${rates.size}")

        return WidgetDisplayData(
            appWidgetId = appWidgetId,
            currencies = rates,
            displayDate = dateStr,
            decimalPlaces = decimalPlaces,
            invertColors = invertColors,
            bgAlpha = bgAlpha,
            cornerRadius = cornerRadius
        )
    }

    protected suspend fun loadDataAndPersistState(context: Context, glanceId: GlanceId, maxCurrencies: Int): WidgetDisplayData {
        val data = loadData(context, glanceId, maxCurrencies)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().also { it.writeWidgetData(data) }
        }
        return data
    }
}

@androidx.glance.GlanceComposable
@androidx.compose.runtime.Composable
fun WidgetCurrencyRow(
    rate: CurrencyRateUiModel,
    decimalPlaces: Int,
    invertColors: Boolean
) {
    val trend = rate.trend
    val trendColor: Color? = when {
        trend == null || trend == 0 -> null
        trend > 0 -> if (invertColors) Color(0xFFD32F2F) else Color(0xFF388E3C)
        else -> if (invertColors) Color(0xFF388E3C) else Color(0xFFD32F2F)
    }
    val valueColor = trendColor ?: Color(0xFF212121)

    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${rate.flagEmoji} ${rate.charCode}",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight()
        )
        androidx.glance.layout.Column(horizontalAlignment = Alignment.End) {
            Text(
                text = rate.todayValue.formatRate(decimalPlaces),
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(valueColor)
                )
            )
            if (rate.tomorrowValue != null) {
                val tomorrowTrend = rate.tomorrowValue.compareTo(rate.todayValue)
                val tomorrowColor = when {
                    tomorrowTrend > 0 -> if (invertColors) Color(0xFFD32F2F) else Color(0xFF388E3C)
                    tomorrowTrend < 0 -> if (invertColors) Color(0xFF388E3C) else Color(0xFFD32F2F)
                    else -> Color(0xFF757575)
                }
                Text(
                    text = "→ ${rate.tomorrowValue.formatRate(decimalPlaces)}",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(tomorrowColor)
                    )
                )
            }
        }
    }
}

internal fun Double.formatRate(decimals: Int) = "%.${decimals}f".format(this)
