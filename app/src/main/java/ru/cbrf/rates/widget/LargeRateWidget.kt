package ru.cbrf.rates.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth

class LargeRateWidget : BaseRateWidget() {

    override val maxCurrencies = 4

    @androidx.glance.GlanceComposable
    @Composable
    override fun CurrencyContent(
        displayData: WidgetDisplayData,
        contentColor: Color,
        secondaryColor: Color,
        mainIntent: Intent
    ) {
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
                        invertColors = displayData.invertColors,
                        contentColor = contentColor,
                        secondaryColor = secondaryColor
                    )
                }
            }
        }
    }
}

class LargeRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LargeRateWidget()
}
