package ru.cbrf.rates.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.cbrf.rates.data.local.prefs.AppLanguage
import ru.cbrf.rates.data.local.prefs.AppPreferences
import ru.cbrf.rates.presentation.navigation.AppNavHost
import ru.cbrf.rates.presentation.theme.CbrfTheme
import ru.cbrf.rates.util.LocaleHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun attachBaseContext(newBase: Context) =
        super.attachBaseContext(LocaleHelper.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by appPreferences.theme.collectAsState(initial = ru.cbrf.rates.data.local.prefs.AppTheme.AUTO)
            // Read initial language from SharedPreferences to match what attachBaseContext already applied
            val initialLanguage = remember {
                val stored = getSharedPreferences(AppPreferences.LANG_PREFS, Context.MODE_PRIVATE)
                    .getString(AppPreferences.KEY_LANGUAGE_SP, "AUTO") ?: "AUTO"
                runCatching { AppLanguage.valueOf(stored) }.getOrDefault(AppLanguage.AUTO)
            }
            val language by appPreferences.language.collectAsState(initial = initialLanguage)
            LaunchedEffect(language) {
                if (language != initialLanguage) {
                    recreate()
                }
            }
            CbrfTheme(theme = theme) {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
