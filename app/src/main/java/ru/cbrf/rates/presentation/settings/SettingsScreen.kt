package ru.cbrf.rates.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.cbrf.rates.R
import ru.cbrf.rates.data.local.prefs.AppLanguage
import ru.cbrf.rates.data.local.prefs.AppTheme
import ru.cbrf.rates.data.local.prefs.UpdateInterval
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Language
            SectionLabel(stringResource(R.string.settings_language))
            EnumDropdown(
                options = AppLanguage.entries,
                selected = state.language,
                label = { lang ->
                    when (lang) {
                        AppLanguage.AUTO -> stringResource(R.string.settings_language_auto)
                        AppLanguage.EN -> stringResource(R.string.settings_language_en)
                        AppLanguage.RU -> stringResource(R.string.settings_language_ru)
                    }
                },
                onSelect = viewModel::setLanguage
            )

            Spacer(Modifier.height(16.dp))

            // Theme
            SectionLabel(stringResource(R.string.settings_theme))
            EnumDropdown(
                options = AppTheme.entries,
                selected = state.theme,
                label = { theme ->
                    when (theme) {
                        AppTheme.AUTO -> stringResource(R.string.settings_theme_auto)
                        AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
                        AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
                    }
                },
                onSelect = viewModel::setTheme
            )

            Spacer(Modifier.height(16.dp))

            // Update interval
            SectionLabel(stringResource(R.string.settings_update_interval))
            EnumDropdown(
                options = UpdateInterval.entries,
                selected = state.updateInterval,
                label = { interval ->
                    when (interval) {
                        UpdateInterval.H1 -> stringResource(R.string.settings_interval_1h)
                        UpdateInterval.H3 -> stringResource(R.string.settings_interval_3h)
                        UpdateInterval.H6 -> stringResource(R.string.settings_interval_6h)
                        UpdateInterval.H12 -> stringResource(R.string.settings_interval_12h)
                        UpdateInterval.H24 -> stringResource(R.string.settings_interval_24h)
                    }
                },
                onSelect = viewModel::setUpdateInterval
            )

            Spacer(Modifier.height(16.dp))

            // Decimal places
            SectionLabel(stringResource(R.string.settings_decimal_places))
            EnumDropdown(
                options = listOf(2, 4),
                selected = state.decimalPlaces,
                label = { places ->
                    if (places == 2) stringResource(R.string.settings_decimals_2)
                    else stringResource(R.string.settings_decimals_4)
                },
                onSelect = viewModel::setDecimalPlaces
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Invert colors
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_color_inversion), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.settings_color_inversion_desc), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.invertColors, onCheckedChange = viewModel::setInvertColors)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Widget appearance
            SectionLabel(stringResource(R.string.settings_widget_appearance))
            Spacer(Modifier.height(8.dp))

            var localAlpha by remember { mutableStateOf(state.widgetBgAlpha) }
            LaunchedEffect(state.widgetBgAlpha) { localAlpha = state.widgetBgAlpha }
            Text(
                text = "${stringResource(R.string.settings_widget_bg_alpha)}: ${(localAlpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = localAlpha,
                onValueChange = { localAlpha = it },
                onValueChangeFinished = { viewModel.setWidgetBgAlpha(localAlpha) },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            var localRadius by remember { mutableStateOf(state.widgetCornerRadius) }
            LaunchedEffect(state.widgetCornerRadius) { localRadius = state.widgetCornerRadius }
            Text(
                text = "${stringResource(R.string.settings_widget_corner_radius)}: ${localRadius.roundToInt()}dp",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = localRadius,
                onValueChange = { localRadius = it },
                onValueChangeFinished = { viewModel.setWidgetCornerRadius(localRadius) },
                valueRange = 0f..24f,
                steps = 23,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
