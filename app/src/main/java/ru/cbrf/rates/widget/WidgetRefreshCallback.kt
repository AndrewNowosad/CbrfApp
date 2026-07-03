package ru.cbrf.rates.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.EntryPointAccessors

class WidgetRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Show a spinner on the tapped widget while the refresh is in flight
        val widget = widgetFor(context, glanceId)
        widget?.let {
            setRefreshing(context, glanceId, true)
            it.update(context, glanceId)
        }
        try {
            val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            ep.refreshTodayRates()(force = true)
        } finally {
            WidgetUpdateHelper.requestUpdate(context)
            // requestUpdate clears the flag when it writes fresh data; this covers the
            // path where loadData failed and nothing was written for the tapped widget.
            widget?.let {
                setRefreshing(context, glanceId, false)
                it.update(context, glanceId)
            }
        }
    }

    private suspend fun widgetFor(context: Context, glanceId: GlanceId): BaseRateWidget? {
        val manager = GlanceAppWidgetManager(context)
        return listOf(SmallRateWidget(), MediumRateWidget(), LargeRateWidget())
            .firstOrNull { glanceId in manager.getGlanceIds(it.javaClass) }
    }

    private suspend fun setRefreshing(context: Context, glanceId: GlanceId, refreshing: Boolean) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().also {
                if (refreshing) it[WidgetStateKeys.REFRESHING] = true
                else it.remove(WidgetStateKeys.REFRESHING)
            }
        }
    }
}
