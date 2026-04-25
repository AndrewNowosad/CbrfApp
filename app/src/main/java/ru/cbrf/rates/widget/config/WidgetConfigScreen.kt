package ru.cbrf.rates.widget.config

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.cbrf.rates.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    viewModel: WidgetConfigViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.widget_config_title)) })
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Hint
            Text(
                text = stringResource(R.string.widget_config_max_hint, state.maxCurrencies),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Selected currencies with reorder controls
            if (state.selectedCodes.isNotEmpty()) {
                Text(
                    text = "Selected (${state.selectedCodes.size}/${state.maxCurrencies})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                state.selectedCodes.forEachIndexed { index, code ->
                    val item = state.items.find { it.charCode == code }
                    if (item != null) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.flagEmoji, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${item.charCode} — ${item.displayName}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (index > 0) {
                                IconButton(onClick = { viewModel.moveUp(code) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                                }
                            }
                            if (index < state.selectedCodes.size - 1) {
                                IconButton(onClick = { viewModel.moveDown(code) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.widget_config_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // Currency list
            val filtered = state.items.filter { item ->
                state.searchQuery.isBlank() ||
                        item.charCode.contains(state.searchQuery, ignoreCase = true) ||
                        item.nameRu.contains(state.searchQuery, ignoreCase = true) ||
                        item.nameEn.contains(state.searchQuery, ignoreCase = true)
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.charCode }) { item ->
                    val canSelect = item.isSelected || state.selectedCodes.size < state.maxCurrencies
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isSelected,
                            onCheckedChange = { if (it || item.isSelected) viewModel.toggleCurrency(item.charCode) },
                            enabled = canSelect || item.isSelected
                        )
                        Text(text = item.flagEmoji, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.charCode,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.widget_config_cancel))
                }
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.save { onSaved() }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.selectedCodes.isNotEmpty()
                ) {
                    Text(stringResource(R.string.widget_config_save))
                }
            }
        }
    }
}
