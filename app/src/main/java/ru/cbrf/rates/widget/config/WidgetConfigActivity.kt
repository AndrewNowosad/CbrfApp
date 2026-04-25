package ru.cbrf.rates.widget.config

import android.appwidget.AppWidgetManager
import android.os.Bundle
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
import javax.inject.Inject

@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val viewModel: WidgetConfigViewModel by viewModels()

    companion object {
        val PARAM_WIDGET_ID = androidx.glance.action.ActionParameters.Key<Int>("widget_id")
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

        viewModel.init(effectiveId)

        // Default result = CANCELED (user might back out)
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
