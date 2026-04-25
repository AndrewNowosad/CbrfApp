package ru.cbrf.rates.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.cbrf.rates.presentation.main.MainScreen
import ru.cbrf.rates.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(onOpenSettings = { navController.navigate(Screen.Settings.route) })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
