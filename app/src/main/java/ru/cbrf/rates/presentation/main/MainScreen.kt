package ru.cbrf.rates.presentation.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import ru.cbrf.rates.R
import ru.cbrf.rates.domain.model.CurrencyRateUiModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.hasError) {
        if (state.hasError) snackbarHostState.showSnackbar("Network error. Showing cached data.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = state.displayDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.select_date))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refresh(force = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Today / Tomorrow chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.displayDate == LocalDate.now(),
                        onClick = { viewModel.jumpToToday() },
                        label = { Text(stringResource(R.string.today)) }
                    )
                    if (state.hasTomorrow) {
                        FilterChip(
                            selected = state.displayDate == LocalDate.now().plusDays(1),
                            onClick = { viewModel.jumpToTomorrow() },
                            label = { Text(stringResource(R.string.tomorrow)) }
                        )
                    }
                }

                // Rates list with horizontal swipe to change date
                var dragAccumulator by remember { mutableFloatStateOf(0f) }
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(state.displayDate) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragAccumulator > 80f) {
                                        viewModel.setDisplayDate(state.displayDate.minusDays(1))
                                    } else if (dragAccumulator < -80f) {
                                        val next = state.displayDate.plusDays(1)
                                        if (!next.isAfter(LocalDate.now().plusDays(1))) {
                                            viewModel.setDisplayDate(next)
                                        }
                                    }
                                    dragAccumulator = 0f
                                },
                                onDragCancel = { dragAccumulator = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragAccumulator += dragAmount
                                }
                            )
                        }
                ) {
                    if (state.isLoading && state.rates.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.loading))
                        }
                    } else if (state.rates.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_rates))
                        }
                    } else {
                        LazyColumn {
                            item {
                                RateListHeader(hasTomorrow = state.rates.any { it.tomorrowValue != null })
                            }
                            items(state.rates, key = { it.charCode }) { rate ->
                                RateRow(
                                    rate = rate,
                                    decimalPlaces = state.decimalPlaces,
                                    invertColors = state.invertColors
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowMillis = tomorrow.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.displayDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= tomorrowMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.setDisplayDate(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.widget_config_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun RateListHeader(hasTomorrow: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.header_currency),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.header_rate),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(90.dp)
        )
        if (hasTomorrow) {
            Text(
                text = stringResource(R.string.tomorrow),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.width(90.dp)
            )
        }
    }
}

@Composable
private fun RateRow(
    rate: CurrencyRateUiModel,
    decimalPlaces: Int,
    invertColors: Boolean
) {
    val isRu = LocalConfiguration.current.locales[0].language == "ru"
    val displayName = if (isRu) rate.nameRu else rate.nameEn
    val trend = rate.trend
    val trendColor = when {
        trend == null -> MaterialTheme.colorScheme.onSurface
        trend > 0 -> if (invertColors) Color(0xFFD32F2F) else Color(0xFF388E3C)
        trend < 0 -> if (invertColors) Color(0xFF388E3C) else Color(0xFFD32F2F)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flag + code + name
        Text(text = rate.flagEmoji, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(text = rate.charCode, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Trend icon
        if (trend != null && trend != 0) {
            Icon(
                imageVector = if (trend > 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = trendColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Today value
        Text(
            text = rate.todayValue.formatRate(decimalPlaces),
            style = MaterialTheme.typography.bodyMedium,
            color = trendColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(90.dp)
        )

        // Tomorrow value
        if (rate.tomorrowValue != null) {
            val tomorrowTrend = rate.tomorrowValue.compareTo(rate.todayValue)
            val tomorrowColor = when {
                tomorrowTrend > 0 -> if (invertColors) Color(0xFFD32F2F) else Color(0xFF388E3C)
                tomorrowTrend < 0 -> if (invertColors) Color(0xFF388E3C) else Color(0xFFD32F2F)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = rate.tomorrowValue.formatRate(decimalPlaces),
                style = MaterialTheme.typography.bodyMedium,
                color = tomorrowColor,
                textAlign = TextAlign.End,
                modifier = Modifier.width(90.dp)
            )
        }
    }
}

private fun Double.formatRate(decimals: Int): String = "%.${decimals}f".format(this)
