package com.leissa.slimtrack.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leissa.slimtrack.data.EntryWithFood
import com.leissa.slimtrack.data.ExerciseEntry
import com.leissa.slimtrack.data.Meal
import com.leissa.slimtrack.ui.components.CalorieProgressBar
import com.leissa.slimtrack.ui.components.MacroStat
import com.leissa.slimtrack.util.dayLabel
import com.leissa.slimtrack.util.defaultMealForNow
import com.leissa.slimtrack.util.gramsText
import com.leissa.slimtrack.util.kcalText
import com.leissa.slimtrack.util.quantityText
import com.leissa.slimtrack.util.servingAmountText

@Composable
fun TodayScreen(viewModel: TodayViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshToday() }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var sheetMeal by rememberSaveable { mutableStateOf(defaultMealForNow().name) }
    var exerciseSheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingEntryId by rememberSaveable { mutableLongStateOf(-1L) }
    val editingEntry = state.entries.firstOrNull { it.entry.id == editingEntryId }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    sheetMeal = defaultMealForNow().name
                    sheetOpen = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add food") },
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
            item { DateHeader(state, viewModel::previousDay, viewModel::nextDay, viewModel::goToToday) }
            item { CalorieCard(state) }
            item { Spacer(Modifier.height(8.dp)) }

            Meal.entries.forEach { meal ->
                item(key = "meal-${meal.name}") {
                    MealHeader(
                        meal = meal,
                        calories = state.caloriesFor(meal),
                        onAdd = {
                            sheetMeal = meal.name
                            sheetOpen = true
                        },
                    )
                }
                val mealEntries = state.entriesFor(meal)
                if (mealEntries.isEmpty()) {
                    item(key = "empty-${meal.name}") {
                        Text(
                            text = "Nothing logged yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                } else {
                    items(mealEntries, key = { "entry-${it.entry.id}" }) { entry ->
                        EntryRow(
                            entry = entry,
                            onEdit = { editingEntryId = entry.entry.id },
                            onDelete = { viewModel.deleteEntry(entry.entry) },
                        )
                    }
                }
            }

            item(key = "exercise-header") {
                SectionHeader(
                    title = "Exercise",
                    detail = if (state.burned > 0) "+${state.burned.kcalText()} kcal" else null,
                    onAdd = { exerciseSheetOpen = true },
                )
            }
            if (state.exercises.isEmpty()) {
                item(key = "exercise-empty") {
                    Text(
                        text = "No exercise logged. It adds to today's calorie budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            } else {
                items(state.exercises, key = { "exercise-${it.id}" }) { entry ->
                    ExerciseRow(entry = entry, onDelete = { viewModel.deleteExercise(entry) })
                }
            }
        }
    }

    if (sheetOpen) {
        AddEntrySheet(
            foods = state.foods,
            initialMeal = Meal.valueOf(sheetMeal),
            onDismiss = { sheetOpen = false },
            onAdd = { foodId, meal, quantity ->
                viewModel.addEntry(foodId, meal, quantity)
                sheetOpen = false
            },
            onAddCustom = { name, servingLabel, calories, protein, carbs, fat, meal, quantity ->
                viewModel.addCustomFood(name, servingLabel, calories, protein, carbs, fat, meal, quantity)
                sheetOpen = false
            },
            onAddProduct = { food, meal, quantity ->
                viewModel.addProduct(food, meal, quantity)
                sheetOpen = false
            },
            onSearchFoods = viewModel::searchFoods,
            onResolveSearchResult = viewModel::resolveSearchResult,
            onLookupBarcode = viewModel::lookupBarcode,
        )
    }

    if (exerciseSheetOpen) {
        AddExerciseSheet(
            weightKg = state.latestWeightKg,
            onDismiss = { exerciseSheetOpen = false },
            onAdd = { type, minutes ->
                viewModel.addExercise(type, minutes)
                exerciseSheetOpen = false
            },
        )
    }

    if (editingEntry != null) {
        EditEntrySheet(
            entry = editingEntry,
            onDismiss = { editingEntryId = -1L },
            onSave = { quantity ->
                viewModel.updateEntryQuantity(editingEntry.entry.id, quantity)
                editingEntryId = -1L
            },
        )
    }
}

@Composable
private fun DateHeader(
    state: TodayUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.date.dayLabel(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.isToday) {
                Text(
                    text = "Let's hit your targets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TextButton(onClick = onToday, contentPadding = PaddingValues(0.dp)) {
                    Text("Jump to today")
                }
            }
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        IconButton(onClick = onNext, enabled = !state.isToday) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun CalorieCard(state: TodayUiState) {
    val remaining = state.remaining
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.calories.kcalText(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = " / ${state.effectiveGoal} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            CalorieProgressBar(value = state.calories, goal = state.effectiveGoal)
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (remaining >= 0) {
                    "${remaining.kcalText()} kcal left"
                } else {
                    "${(-remaining).kcalText()} kcal over target"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (remaining >= 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (state.burned > 0) {
                Text(
                    text = "Goal ${state.goals.calories} + ${state.burned.kcalText()} burned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroStat("Protein", state.protein, state.goals.proteinGrams, Modifier.weight(1f))
                MacroStat("Carbs", state.carbs, state.goals.carbsGrams, Modifier.weight(1f))
                MacroStat("Fat", state.fat, state.goals.fatGrams, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, detail: String?, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = "Add $title")
        }
    }
}

@Composable
private fun MealHeader(meal: Meal, calories: Double, onAdd: () -> Unit) {
    SectionHeader(
        title = meal.label,
        detail = "${calories.kcalText()} kcal",
        onAdd = onAdd,
    )
}

@Composable
private fun EntryRow(entry: EntryWithFood, onEdit: () -> Unit, onDelete: () -> Unit) {
    val amount = servingAmountText(entry.food.servingLabel, entry.entry.quantity)
        ?: "${entry.entry.quantity.quantityText()} × ${entry.food.servingLabel}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.food.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$amount · " +
                    "P ${entry.protein.gramsText()} · C ${entry.carbs.gramsText()} · F ${entry.fat.gramsText()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.calories.kcalText(),
            style = MaterialTheme.typography.labelLarge,
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove ${entry.food.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExerciseRow(entry: ExerciseEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.activity.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${entry.minutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "+${entry.caloriesBurned.kcalText()}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove ${entry.activity.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
