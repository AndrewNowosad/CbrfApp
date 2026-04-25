package ru.cbrf.rates.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
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
        val data = loadData(context, id, maxCurrencies = 4)
        val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, data.appWidgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        provideContent {
            val bgColor = Color.White.copy(alpha = data.bgAlpha)
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .cornerRadius(data.cornerRadius.dp)
                    .padding(8.dp)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.displayDate,
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

                    if (data.currencies.isNotEmpty()) {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .clickable(actionStartActivity(mainIntent))
                        ) {
                            data.currencies.forEach { rate ->
                                WidgetCurrencyRow(
                                    rate = rate,
                                    decimalPlaces = data.decimalPlaces,
                                    invertColors = data.invertColors
                                )
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
