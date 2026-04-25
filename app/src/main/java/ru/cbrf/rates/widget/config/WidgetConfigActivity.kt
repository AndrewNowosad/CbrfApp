package ru.cbrf.rates.widget.config

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import ru.cbrf.rates.widget.LargeRateWidgetReceiver
import ru.cbrf.rates.widget.MediumRateWidgetReceiver
import ru.cbrf.rates.widget.SmallRateWidgetReceiver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import ru.cbrf.rates.data.local.prefs.AppPreferences
import ru.cbrf.rates.data.local.prefs.AppTheme
import ru.cbrf.rates.presentation.theme.CbrfTheme
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val viewModel: WidgetConfigViewModel by viewModels()

    companion object {
        val PARAM_WIDGET_ID = androidx.glance.action.ActionParameters.Key<Int>("widget_id")
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences(AppPreferences.LANG_PREFS, Context.MODE_PRIVATE)
            .getString(AppPreferences.KEY_LANGUAGE_SP, "AUTO") ?: "AUTO"
        val locale: Locale? = when (lang) {
            "RU" -> Locale("ru")
            "EN" -> Locale("en")
            else -> null
        }
        if (locale != null) {
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        // Also handle being opened from the widget's gear icon (passing id via intent extra)
        val fromGlanceId = intent.getIntExtra("widget_id", AppWidgetManager.INVALID_APPWIDGET_ID)
        val effectiveId = if (fromGlanceId != AppWidgetManager.INVALID_APPWIDGET_ID) fromGlanceId else appWidgetId

        if (effectiveId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val providerClass = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(effectiveId)?.provider?.className ?: ""
        val sizeHint = when {
            providerClass.contains(SmallRateWidgetReceiver::class.java.simpleName) -> "SMALL"
            providerClass.contains(MediumRateWidgetReceiver::class.java.simpleName) -> "MEDIUM"
            providerClass.contains(LargeRateWidgetReceiver::class.java.simpleName) -> "LARGE"
            else -> null
        }
        viewModel.init(effectiveId, sizeHint)

        // Default result = CANCELED

        setResult(RESULT_CANCELED, android.content.Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, effectiveId))

        enableEdgeToEdge()
        setContent {
            val theme by appPreferences.theme.collectAsState(initial = AppTheme.AUTO)
            CbrfTheme(theme = theme) {
                WidgetConfigScreen(
                    viewModel = viewModel,
                    onSaved = {
                        setResult(RESULT_OK, android.content.Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, effectiveId))
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}
