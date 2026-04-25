package ru.cbrf.rates.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import ru.cbrf.rates.presentation.MainActivity
import ru.cbrf.rates.widget.config.WidgetConfigActivity

class LargeRateWidget : BaseRateWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadDataAndPersistState(context, id, maxCurrencies = 4)
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
            val bgColor = Color.White.copy(alpha = displayData.bgAlpha)
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
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = ColorProvider(Color(0xFF757575))
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "↻",
                            style = TextStyle(fontSize = 14.sp),
                            modifier = GlanceModifier.clickable(actionRunCallback<WidgetRefreshCallback>())
                        )
                        Text(
                            text = " ⚙",
                            style = TextStyle(fontSize = 14.sp),
                            modifier = GlanceModifier.clickable(actionStartActivity(configIntent))
                        )
                    }

                    if (displayData.currencies.isNotEmpty()) {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .clickable(actionStartActivity(mainIntent))
                        ) {
                            displayData.currencies.forEach { rate ->
                                Box(
                                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    WidgetCurrencyRow(
                                        rate = rate,
                                        decimalPlaces = displayData.decimalPlaces,
                                        invertColors = displayData.invertColors
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .clickable(actionStartActivity(configIntent)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Tap ⚙ to configure",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = ColorProvider(Color(0xFF757575))
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class LargeRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LargeRateWidget()
}
