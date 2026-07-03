package ru.cbrf.rates.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

object WidgetUpdateHelper {

    suspend fun requestUpdate(context: Context) {
        updateWidgetType(context, SmallRateWidget(), 1)
        updateWidgetType(context, MediumRateWidget(), 2)
        updateWidgetType(context, LargeRateWidget(), 4)
    }

    private suspend fun updateWidgetType(
        context: Context,
        widget: BaseRateWidget,
        maxCurrencies: Int
    ) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(widget.javaClass)
        for (glanceId in glanceIds) {
            val data = widget.loadData(context, glanceId, maxCurrencies) ?: continue
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().also {
                    it.writeWidgetData(data)
                    it.remove(WidgetStateKeys.REFRESHING)
                }
            }
            widget.update(context, glanceId)
        }
    }
}
