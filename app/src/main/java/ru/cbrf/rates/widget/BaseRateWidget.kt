package ru.cbrf.rates.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import ru.cbrf.rates.domain.model.CurrencyRateUiModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

abstract class BaseRateWidget : GlanceAppWidget() {

    protected suspend fun loadData(context: Context, glanceId: GlanceId, maxCurrencies: Int): WidgetDisplayData {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val widgetPrefs = ep.widgetPreferences()
        val appPrefs = ep.appPreferences()
        val getRates = ep.getRatesForDisplay()

        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)

        val currencies = widgetPrefs.getCurrenciesOnce(appWidgetId).take(maxCurrencies)
        val decimalPlaces = appPrefs.decimalPlaces.first()
        val invertColors = appPrefs.invertColors.first()
        val bgAlpha = appPrefs.widgetBgAlpha.first()
        val cornerRadius = appPrefs.widgetCornerRadius.first()

        val today = LocalDate.now()
        val rates = if (currencies.isEmpty()) {
            emptyList()
        } else {
            getRates(today)
                .filter { it.charCode in currencies }
                .sortedBy { currencies.indexOf(it.charCode) }
        }

        val dateStr = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))

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
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
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
                    fontSize = 13.sp,
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
