package ru.cbrf.rates.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors

class WidgetRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        ep.refreshTodayRates()(force = true)
        SmallRateWidget().updateAll(context)
        MediumRateWidget().updateAll(context)
        LargeRateWidget().updateAll(context)
    }
}
