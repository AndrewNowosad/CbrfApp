package ru.cbrf.rates.util

import android.content.Context
import android.content.res.Configuration
import ru.cbrf.rates.data.local.prefs.AppPreferences
import java.util.Locale

object LocaleHelper {
    fun wrap(base: Context): Context {
        val locale = selectedLocaleOrNull(base)
        if (locale != null) {
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            return base.createConfigurationContext(config)
        }
        return base
    }

    /**
     * Locale chosen in app settings, or the system default when set to AUTO.
     * Usable outside Activity contexts (e.g. Glance widgets).
     */
    fun selectedLocale(context: Context): Locale =
        selectedLocaleOrNull(context) ?: Locale.getDefault()

    private fun selectedLocaleOrNull(context: Context): Locale? {
        val lang = context.getSharedPreferences(AppPreferences.LANG_PREFS, Context.MODE_PRIVATE)
            .getString(AppPreferences.KEY_LANGUAGE_SP, "AUTO") ?: "AUTO"
        return when (lang) {
            "RU" -> Locale("ru")
            "EN" -> Locale("en")
            else -> null
        }
    }
}
