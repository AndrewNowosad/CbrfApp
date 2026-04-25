package ru.cbrf.rates.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme { LIGHT, DARK, AUTO }
enum class UpdateInterval(val hours: Long) {
    H1(1), H3(3), H6(6), H12(12), H24(24)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_INTERVAL = stringPreferencesKey("update_interval")
        private val KEY_DECIMALS = intPreferencesKey("decimal_places")
        private val KEY_INVERT_COLORS = booleanPreferencesKey("invert_colors")
        private val KEY_WIDGET_BG_ALPHA = floatPreferencesKey("widget_bg_alpha")
        private val KEY_WIDGET_CORNER_RADIUS = floatPreferencesKey("widget_corner_radius")
    }

    val theme: Flow<AppTheme> = store.data.map { prefs ->
        runCatching { AppTheme.valueOf(prefs[KEY_THEME] ?: "AUTO") }.getOrDefault(AppTheme.AUTO)
    }

    val updateInterval: Flow<UpdateInterval> = store.data.map { prefs ->
        runCatching { UpdateInterval.valueOf(prefs[KEY_INTERVAL] ?: "H1") }.getOrDefault(UpdateInterval.H1)
    }

    val decimalPlaces: Flow<Int> = store.data.map { prefs ->
        prefs[KEY_DECIMALS] ?: 4
    }

    val invertColors: Flow<Boolean> = store.data.map { prefs ->
        prefs[KEY_INVERT_COLORS] ?: false
    }

    val widgetBgAlpha: Flow<Float> = store.data.map { prefs ->
        prefs[KEY_WIDGET_BG_ALPHA] ?: 0.85f
    }

    val widgetCornerRadius: Flow<Float> = store.data.map { prefs ->
        prefs[KEY_WIDGET_CORNER_RADIUS] ?: 16f
    }

    suspend fun setTheme(theme: AppTheme) = store.edit { it[KEY_THEME] = theme.name }
    suspend fun setUpdateInterval(interval: UpdateInterval) = store.edit { it[KEY_INTERVAL] = interval.name }
    suspend fun setDecimalPlaces(places: Int) = store.edit { it[KEY_DECIMALS] = places }
    suspend fun setInvertColors(invert: Boolean) = store.edit { it[KEY_INVERT_COLORS] = invert }
    suspend fun setWidgetBgAlpha(alpha: Float) = store.edit { it[KEY_WIDGET_BG_ALPHA] = alpha }
    suspend fun setWidgetCornerRadius(radius: Float) = store.edit { it[KEY_WIDGET_CORNER_RADIUS] = radius }
}
