package ru.cbrf.rates.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.cbrf.rates.data.local.prefs.AppPreferences
import ru.cbrf.rates.presentation.navigation.AppNavHost
import ru.cbrf.rates.presentation.theme.CbrfTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by appPreferences.theme.collectAsState(initial = ru.cbrf.rates.data.local.prefs.AppTheme.AUTO)
            CbrfTheme(theme = theme) {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
