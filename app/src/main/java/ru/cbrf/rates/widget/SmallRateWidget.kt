package ru.cbrf.rates.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import android.content.res.Configuration
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import ru.cbrf.rates.data.local.prefs.WidgetBgColorMode
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ru.cbrf.rates.presentation.MainActivity
import ru.cbrf.rates.widget.config.WidgetConfigActivity

class SmallRateWidget : BaseRateWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadDataAndPersistState(context, id, maxCurrencies = 1)
        val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, data.appWidgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        provideContent {
            val prefs = currentState<Preferences>()
            val displayData = prefs.readWidgetData() ?: data
            val isDark = (LocalContext.current.resources.configuration.uiMode
                    and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val bgBase = when (displayData.bgColorMode) {
                WidgetBgColorMode.LIGHT -> Color.White
                WidgetBgColorMode.DARK -> Color(0xFF1C1B1F)
                WidgetBgColorMode.AUTO -> if (isDark) Color(0xFF1C1B1F) else Color.White
            }
            val bgColor = bgBase.copy(alpha = displayData.bgAlpha)
            val isDarkBg = when (displayData.bgColorMode) {
                WidgetBgColorMode.DARK -> true
                WidgetBgColorMode.LIGHT -> false
                WidgetBgColorMode.AUTO -> isDark
            }
            val contentColor = if (isDarkBg) Color(0xFFE1E1E1) else Color(0xFF212121)
            val secondaryColor = if (isDarkBg) Color(0xFF9E9E9E) else Color(0xFF757575)

            val size = LocalSize.current
            val minDim = minOf(size.width, size.height)
            // 3 tiers: tiny (<80dp) / compact (80-130dp) / full (>130dp)
            val isTiny = minDim < 80.dp
            val isFull = minDim > 130.dp
            val codeTextSize = when { isTiny -> 9.sp; isFull -> 13.sp; else -> 11.sp }
            val valueTextSize = when { isTiny -> 12.sp; isFull -> 20.sp; else -> 15.sp }
            val tomorrowTextSize = when { isTiny -> 8.sp; else -> 10.sp }
            val showFlag = !isTiny
            val showTomorrow = isFull
            val pad = if (isTiny) 4.dp else 8.dp
            val headerSize = when { isTiny -> 8.sp; isFull -> 10.sp; else -> 9.sp }
            val iconSize = when { isTiny -> 10.sp; isFull -> 14.sp; else -> 11.sp }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(bgColor)
                    .cornerRadius(displayData.cornerRadius.dp)
                    .padding(pad)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayData.displayDate,
                            style = TextStyle(fontSize = headerSize, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "↻",
                            style = TextStyle(fontSize = iconSize, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.clickable(actionRunCallback<WidgetRefreshCallback>())
                        )
                        Text(
                            text = " ⚙",
                            style = TextStyle(fontSize = iconSize, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.clickable(actionStartActivity(configIntent))
                        )
                    }

                    if (displayData.currencies.isNotEmpty()) {
                        val rate = displayData.currencies.first()
                        val trend = rate.trend
                        val trendColor: Color? = when {
                            trend == null || trend == 0 -> null
                            trend > 0 -> if (displayData.invertColors) Color(0xFFD32F2F) else Color(0xFF388E3C)
                            else -> if (displayData.invertColors) Color(0xFF388E3C) else Color(0xFFD32F2F)
                        }
                        val valueColor = trendColor ?: contentColor

                        Spacer(GlanceModifier.defaultWeight())
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(mainIntent)),
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
                            Text(
                                text = rate.todayValue.formatRate(displayData.decimalPlaces),
                                style = TextStyle(
                                    fontSize = valueTextSize,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(valueColor)
                                )
                            )
                            if (showTomorrow && rate.tomorrowValue != null) {
                                val tomorrowTrend = rate.tomorrowValue.compareTo(rate.todayValue)
                                val tomorrowColor = when {
                                    tomorrowTrend > 0 -> if (displayData.invertColors) Color(0xFFD32F2F) else Color(0xFF388E3C)
                                    tomorrowTrend < 0 -> if (displayData.invertColors) Color(0xFF388E3C) else Color(0xFFD32F2F)
                                    else -> secondaryColor
                                }
                                Text(
                                    text = "→ ${rate.tomorrowValue.formatRate(displayData.decimalPlaces)}",
                                    style = TextStyle(fontSize = tomorrowTextSize, color = ColorProvider(tomorrowColor))
                                )
                            }
                        }
                        Spacer(GlanceModifier.defaultWeight())
                    } else {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .clickable(actionStartActivity(configIntent)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚙",
                                style = TextStyle(fontSize = iconSize, color = ColorProvider(secondaryColor))
                            )
                        }
                    }
                }
            }
        }
    }
}

class SmallRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SmallRateWidget()
}
