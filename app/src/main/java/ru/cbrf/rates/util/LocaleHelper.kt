package ru.cbrf.rates.util

import android.content.Context
import android.content.res.Configuration
import ru.cbrf.rates.data.local.prefs.AppPreferences
import java.util.Locale

object LocaleHelper {
    fun wrap(base: Context): Context {
        val lang = base.getSharedPreferences(AppPreferences.LANG_PREFS, Context.MODE_PRIVATE)
            .getString(AppPreferences.KEY_LANGUAGE_SP, "AUTO") ?: "AUTO"
        val locale: Locale? = when (lang) {
            "RU" -> Locale("ru")
            "EN" -> Locale("en")
            else -> null
        }
        if (locale != null) {
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            return base.createConfigurationContext(config)
        }
        return base
    }
}
