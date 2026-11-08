@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.leissa.slimtrack.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leissa.slimtrack.data.Energy
import com.leissa.slimtrack.data.UnitSystem
import com.leissa.slimtrack.data.WeightEntry
import com.leissa.slimtrack.util.displayHeight
import com.leissa.slimtrack.util.displayWeight
import com.leissa.slimtrack.util.displayWeightWithUnit
import com.leissa.slimtrack.util.dayLabel
import com.leissa.slimtrack.util.parseWeightToKg
import com.leissa.slimtrack.util.weightUnitLabel
import java.time.LocalDate

private enum class ChartRange(val label: String, val days: Long?) {
    MONTH("1M", 30),
    QUARTER("3M", 90),
    HALF_YEAR("6M", 182),
    ALL("All", null),
}

@Composable
fun WeightScreen(viewModel: WeightViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var rangeName by rememberSaveable { mutableStateOf(ChartRange.QUARTER.name) }
    var showDaily by rememberSaveable { mutableStateOf(true) }
    var showAvg3 by rememberSaveable { mutableStateOf(true) }
    var showAvg7 by rememberSaveable { mutableStateOf(true) }

    val range = ChartRange.valueOf(rangeName)
    val visibleEntries = remember(state.entries, range) {
        val cutoff = range.days?.let { days ->
            (state.entries.lastOrNull()?.epochDay ?: LocalDate.now().toEpochDay()) - days
        }
        if (cutoff == null) state.entries else state.entries.filter { it.epochDay >= cutoff }
    }
    val points = remember(visibleEntries) { weightPoints(visibleEntries) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialogOpen = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Log weight") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
        ) {
            item {
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Height ${displayHeight(state.profile.heightCm, state.profile.unitSystem)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                CurrentWeightCard(state)
                Spacer(Modifier.height(16.dp))
            }

            if (state.entries.isEmpty()) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                text = "No weight yet",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Tap \"Log weight\" to add your first measurement. " +
                                    "Weighing yourself at the same time of day gives the cleanest trend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ChartRange.entries.forEach { option ->
                                    FilterChip(
                                        selected = range == option,
                                        onClick = { rangeName = option.name },
                                        label = { Text(option.label) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = showDaily,
                                    onClick = { showDaily = !showDaily },
                                    label = { Text("Daily") },
                                )
                                FilterChip(
                                    selected = showAvg3,
                                    onClick = { showAvg3 = !showAvg3 },
                                    label = { Text("3-day avg") },
                                )
                                FilterChip(
                                    selected = showAvg7,
                                    onClick = { showAvg7 = !showAvg7 },
                                    label = { Text("7-day avg") },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (showDaily || showAvg3 || showAvg7) {
                                WeightChart(
                                    points = points,
                                    unit = state.profile.unitSystem,
                                    showDaily = showDaily,
                                    showAvg3 = showAvg3,
                                    showAvg7 = showAvg7,
                                )
                            } else {
                                Text(
                                    text = "Pick at least one series to show.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 40.dp),
                                )
                            }
                            Text(
                                text = "Weight in ${weightUnitLabel(state.profile.unitSystem)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                }

                items(state.entries.reversed(), key = { it.id }) { entry ->
                    WeightRow(
                        entry = entry,
                        unit = state.profile.unitSystem,
                        bmi = Energy.bmi(entry.weightKg, state.profile.heightCm),
                        onDelete = { viewModel.delete(entry) },
                    )
                }
            }
        }
    }

    if (dialogOpen) {
        AddWeightDialog(
            unit = state.profile.unitSystem,
            initialWeightKg = state.latest?.weightKg,
            existing = state.entries,
            onDismiss = { dialogOpen = false },
            onSave = { date, kg ->
                viewModel.logWeight(date, kg)
                dialogOpen = false
            },
        )
    }
}

@Composable
private fun CurrentWeightCard(state: WeightUiState) {
    val unit = state.profile.unitSystem
    val locale = LocalConfiguration.current.locales[0]
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.latest?.let { displayWeight(it.weightKg, unit) } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = " ${weightUnitLabel(unit)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "BMI",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (state.latest == null) "—" else "%.1f".format(locale, state.bmi),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = Energy.bmiCategory(state.bmi),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = displayWeightWithUnit(state.profile.targetWeightKg, unit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "BMI %.1f · ${Energy.bmiCategory(state.targetBmi)}"
                            .format(locale, state.targetBmi),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val progress = state.progress
            if (progress != null) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                    drawStopIndicator = {},
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${(progress * 100).toInt()}% of the way to your target",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeightRow(
    entry: WeightEntry,
    unit: UnitSystem,
    bmi: Double,
    onDelete: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = LocalDate.ofEpochDay(entry.epochDay).dayLabel(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "BMI %.1f".format(locale, bmi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = displayWeightWithUnit(entry.weightKg, unit),
            style = MaterialTheme.typography.labelLarge,
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete weight",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddWeightDialog(
    unit: UnitSystem,
    initialWeightKg: Double?,
    existing: List<WeightEntry>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double) -> Unit,
) {
    var dateEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val existingForDate = existing.firstOrNull { it.epochDay == dateEpochDay }
    var weightText by rememberSaveable(dateEpochDay, initialWeightKg) {
        val kg = existingForDate?.weightKg ?: initialWeightKg
        mutableStateOf(kg?.let { displayWeight(it, unit) } ?: "")
    }
    val parsedKg = parseWeightToKg(weightText, unit)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { dateEpochDay -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                    }
                    Text(
                        text = date.dayLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(
                        onClick = {
                            if (dateEpochDay < LocalDate.now().toEpochDay()) dateEpochDay += 1
                        },
                        enabled = dateEpochDay < LocalDate.now().toEpochDay(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        weightText = input.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    label = { Text("Weight") },
                    suffix = { Text(weightUnitLabel(unit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existingForDate != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This replaces the entry for that day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedKg?.let { onSave(date, it) } },
                enabled = parsedKg != null,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
