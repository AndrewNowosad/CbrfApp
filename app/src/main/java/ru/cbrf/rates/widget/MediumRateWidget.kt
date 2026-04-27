package ru.cbrf.rates.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
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
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ru.cbrf.rates.data.local.prefs.WidgetBgColorMode
import ru.cbrf.rates.presentation.MainActivity
import ru.cbrf.rates.widget.config.WidgetConfigActivity

class MediumRateWidget : BaseRateWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadDataAndPersistState(context, id, maxCurrencies = 2)
        val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, data?.appWidgetId ?: 0)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        provideContent {
            val prefs = currentState<Preferences>()
            val displayData = prefs.readWidgetData() ?: data ?: return@provideContent
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
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(bgColor)
                    .cornerRadius(displayData.cornerRadius.dp)
                    .padding(8.dp)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayData.displayDate,
                            style = TextStyle(fontSize = 11.sp, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "↻",
                            style = TextStyle(fontSize = 16.sp, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.clickable(actionRunCallback<WidgetRefreshCallback>())
                        )
                        Text(
                            text = " ⚙",
                            style = TextStyle(fontSize = 16.sp, color = ColorProvider(secondaryColor)),
                            modifier = GlanceModifier.clickable(actionStartActivity(configIntent))
                        )
                    }

                    if (displayData.currencies.isNotEmpty()) {
                        Spacer(GlanceModifier.defaultWeight())
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity(mainIntent))
                        ) {
                            displayData.currencies.forEach { rate ->
                                WidgetCurrencyRow(
                                    rate = rate,
                                    decimalPlaces = displayData.decimalPlaces,
                                    invertColors = displayData.invertColors,
                                    contentColor = contentColor,
                                    secondaryColor = secondaryColor,
                                    verticalPadding = 2.dp
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
                                "Tap ⚙ to configure",
                                style = TextStyle(fontSize = 11.sp, color = ColorProvider(secondaryColor))
                            )
                        }
                    }
                }
            }
        }
    }
}

class MediumRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MediumRateWidget()
}
