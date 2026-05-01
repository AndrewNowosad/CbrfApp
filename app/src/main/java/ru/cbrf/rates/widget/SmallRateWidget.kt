package ru.cbrf.rates.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class SmallRateWidget : BaseRateWidget() {

    override val maxCurrencies = 1

    @androidx.glance.GlanceComposable
    @Composable
    override fun headerConfig(): HeaderConfig {
        val size = LocalSize.current
        val minDim = minOf(size.width, size.height)
        val isTiny = minDim < 80.dp
        val isFull = minDim > 130.dp
        return HeaderConfig(
            headerSize = when { isTiny -> 9.sp; isFull -> 11.sp; else -> 10.sp },
            iconSize = when { isTiny -> 11.sp; isFull -> 16.sp; else -> 12.sp },
            padding = if (isTiny) 4.dp else 8.dp
        )
    }

    @androidx.glance.GlanceComposable
    @Composable
    override fun EmptyState(
        secondaryColor: Color,
        configIntent: Intent,
        headerConfig: HeaderConfig
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(configIntent)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚙",
                style = TextStyle(fontSize = headerConfig.iconSize, color = ColorProvider(secondaryColor))
            )
        }
    }

    @androidx.glance.GlanceComposable
    @Composable
    override fun CurrencyContent(
        displayData: WidgetDisplayData,
        contentColor: Color,
        secondaryColor: Color,
        mainIntent: Intent
    ) {
        val size = LocalSize.current
        val minDim = minOf(size.width, size.height)
        val isTiny = minDim < 80.dp
        val isFull = minDim > 130.dp
        val codeTextSize = when { isTiny -> 10.sp; isFull -> 15.sp; else -> 12.sp }
        val valueTextSize = when { isTiny -> 13.sp; isFull -> 22.sp; else -> 17.sp }
        val tomorrowTextSize = when { isTiny -> 9.sp; else -> 11.sp }
        val showFlag = !isTiny
        val showTomorrow = isFull

        val rate = displayData.currencies.first()
        val trend = rate.trend
        val trendColor: Color? = when {
            trend == null || trend == 0 -> null
            trend > 0 -> if (displayData.invertColors) Color(0xFFD32F2F) else Color(0xFF388E3C)
            else -> if (displayData.invertColors) Color(0xFF388E3C) else Color(0xFFD32F2F)
        }
        val valueColor = trendColor ?: contentColor

        Box(
            modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (showFlag) "${rate.flagEmoji} ${rate.charCode}" else rate.charCode,
                    style = TextStyle(
                        fontSize = codeTextSize,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(contentColor)
                    )
                )
                if (showTomorrow && rate.tomorrowValue != null) {
                    val tomorrowTrend = rate.tomorrowValue.compareTo(rate.todayValue)
                    val tomorrowColor = when {
                        tomorrowTrend > 0 -> if (displayData.invertColors) Color(0xFFD32F2F) else Color(0xFF388E3C)
                        tomorrowTrend < 0 -> if (displayData.invertColors) Color(0xFF388E3C) else Color(0xFFD32F2F)
                        else -> secondaryColor
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rate.todayValue.formatRate(displayData.decimalPlaces),
                            style = TextStyle(
                                fontSize = valueTextSize,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(valueColor)
                            )
                        )
                        Text(
                            text = "→ ${rate.tomorrowValue.formatRate(displayData.decimalPlaces)}",
                            style = TextStyle(fontSize = tomorrowTextSize, color = ColorProvider(tomorrowColor)),
                            modifier = GlanceModifier.padding(start = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = rate.todayValue.formatRate(displayData.decimalPlaces),
                        style = TextStyle(
                            fontSize = valueTextSize,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(valueColor)
                        )
                    )
                }
            }
        }
    }
}

class SmallRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SmallRateWidget()
}
