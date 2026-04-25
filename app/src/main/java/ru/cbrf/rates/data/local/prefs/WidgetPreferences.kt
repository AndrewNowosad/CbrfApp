package ru.cbrf.rates.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_prefs")

@Singleton
class WidgetPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.widgetDataStore

    private fun currencyKey(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_currencies")
    private fun sizeKey(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_size")

    fun getCurrencies(appWidgetId: Int): Flow<List<String>> = store.data.map { prefs ->
        prefs[currencyKey(appWidgetId)]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun getCurrenciesOnce(appWidgetId: Int): List<String> =
        getCurrencies(appWidgetId).first()

    suspend fun setCurrencies(appWidgetId: Int, currencies: List<String>) {
        store.edit { prefs ->
            prefs[currencyKey(appWidgetId)] = currencies.joinToString(",")
        }
    }

    suspend fun getSize(appWidgetId: Int): String? = store.data.map { it[sizeKey(appWidgetId)] }.first()

    suspend fun setSize(appWidgetId: Int, size: String) {
        store.edit { it[sizeKey(appWidgetId)] = size }
    }

    suspend fun removeWidget(appWidgetId: Int) {
        store.edit { prefs ->
            prefs.remove(currencyKey(appWidgetId))
            prefs.remove(sizeKey(appWidgetId))
        }
    }
}
