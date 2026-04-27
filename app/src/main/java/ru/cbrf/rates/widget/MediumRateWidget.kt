package ru.cbrf.rates.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth

class MediumRateWidget : BaseRateWidget() {

    override val maxCurrencies = 2

    @androidx.glance.GlanceComposable
    @Composable
    override fun CurrencyContent(
        displayData: WidgetDisplayData,
        contentColor: Color,
        secondaryColor: Color,
        mainIntent: Intent
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent)),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
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
        }
    }
}

class MediumRateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MediumRateWidget()
}
