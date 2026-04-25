package ru.cbrf.rates.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

class WidgetRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        ep.refreshTodayRates()(force = true)
        WidgetUpdateHelper.requestUpdate(context)
    }
}
