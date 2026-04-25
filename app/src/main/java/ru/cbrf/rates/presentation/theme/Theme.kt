package ru.cbrf.rates.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import ru.cbrf.rates.data.local.prefs.AppTheme

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun CbrfTheme(
    theme: AppTheme = AppTheme.AUTO,
    content: @Composable () -> Unit
) {
    val dark = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.AUTO -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
