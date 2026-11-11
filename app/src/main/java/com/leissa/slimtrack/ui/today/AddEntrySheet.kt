@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.leissa.slimtrack.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leissa.slimtrack.data.Food
import com.leissa.slimtrack.data.FoodSearchOutcome
import com.leissa.slimtrack.data.FoodSearchResult
import com.leissa.slimtrack.data.FoodSource
import com.leissa.slimtrack.data.Meal
import com.leissa.slimtrack.ui.scan.BarcodeScannerDialog
import com.leissa.slimtrack.util.gramsText
import com.leissa.slimtrack.util.kcalText
import com.leissa.slimtrack.util.toDecimalOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class AddMode(val label: String) {
    MY_FOODS("My foods"),
    DATABASES("Databases"),
    SCAN("Scan"),
}

@Composable
fun AddEntrySheet(
    foods: List<Food>,
    initialModeName: String?,
    onModeSelected: (String) -> Unit,
    initialMeal: Meal,
    onDismiss: () -> Unit,
    onRemoveFood: (Food) -> Unit,
    onAdd: (foodId: Long, meal: Meal, quantity: Double) -> Unit,
    onAddCustom: (
        name: String,
        servingLabel: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        meal: Meal,
        quantity: Double,
    ) -> Unit,
    onAddProduct: (food: Food, meal: Meal, quantity: Double) -> Unit,
    onSearchFoods: suspend (String) -> FoodSearchOutcome,
    onResolveSearchResult: suspend (FoodSearchResult) -> Food?,
    onLookupBarcode: suspend (String) -> FoodSearchResult?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val startMode = AddMode.entries.firstOrNull { it.name == initialModeName } ?: AddMode.MY_FOODS
    var modeName by rememberSaveable { mutableStateOf(startMode.name) }
    // Each tab keeps its own search text, so switching tabs never wipes what was typed.
    var localQuery by rememberSaveable { mutableStateOf("") }
    var databaseQuery by rememberSaveable { mutableStateOf("") }
    var selectedFoodId by rememberSaveable { mutableStateOf<Long?>(null) }
    var mealName by rememberSaveable { mutableStateOf(initialMeal.name) }
    var quantity by rememberSaveable { mutableDoubleStateOf(1.0) }
    var customMode by rememberSaveable { mutableStateOf(false) }

    var databaseResults by remember { mutableStateOf<List<FoodSearchResult>>(emptyList()) }
    var databaseLoading by remember { mutableStateOf(false) }
    var databaseNote by remember { mutableStateOf<String?>(null) }
    var databaseError by remember { mutableStateOf<String?>(null) }
    var resolving by remember { mutableStateOf<String?>(null) }
    var scanLoading by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var pendingProduct by remember { mutableStateOf<FoodSearchResult?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var manualBarcode by rememberSaveable { mutableStateOf("") }

    // Requests are tagged so an answer that arrives after the user started another
    // search, changed tabs or tapped another product is ignored instead of displayed.
    var searchRequest by remember { mutableIntStateOf(0) }
    var resolveRequest by remember { mutableIntStateOf(0) }
    var lookupRequest by remember { mutableIntStateOf(0) }
    var resolveJob by remember { mutableStateOf<Job?>(null) }

    val mode = AddMode.entries.firstOrNull { it.name == modeName } ?: AddMode.MY_FOODS
    val meal = Meal.valueOf(mealName)
    val selectedFood = foods.firstOrNull { it.id == selectedFoodId }
    val filteredFoods = remember(foods, localQuery) {
        val needle = localQuery.trim()
        if (needle.isEmpty()) {
            foods
        } else {
            foods.filter { it.name.contains(needle, ignoreCase = true) }
        }
    }

    /**
     * Drops taps that belonged to a tab the user just left: a launched scan lookup or
     * a product still loading must not take over the sheet while another tab is shown.
     * Search results are kept, so returning to a tab finds its text and hits intact.
     */
    fun cancelPendingPicks() {
        resolveRequest++
        lookupRequest++
        resolveJob?.cancel()
        resolveJob = null
        resolving = null
        scanLoading = false
    }

    fun searchDatabases() {
        val term = databaseQuery.trim()
        if (term.isEmpty()) return
        val request = ++searchRequest
        databaseLoading = true
        databaseError = null
        databaseNote = null
        scope.launch {
            try {
                val outcome = onSearchFoods(term)
                if (request != searchRequest) return@launch
                databaseResults = outcome.results
                if (outcome.onlineUnavailable) {
                    databaseNote = "Online databases unavailable — showing German foods (BLS) only."
                }
                if (outcome.results.isEmpty()) {
                    databaseError = "No foods found for \"$term\"."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == searchRequest) {
                    databaseError = "Search failed. Check your connection."
                }
            } finally {
                if (request == searchRequest) databaseLoading = false
            }
        }
    }

    fun selectSearchResult(result: FoodSearchResult) {
        val ready = result.food
        if (ready != null) {
            pendingProduct = result
            return
        }
        // Only the last tapped product may open the editor.
        resolveJob?.cancel()
        val request = ++resolveRequest
        resolving = result.reference ?: result.name
        databaseError = null
        resolveJob = scope.launch {
            try {
                val food = onResolveSearchResult(result)
                if (request != resolveRequest) return@launch
                if (food == null) {
                    databaseError = "Couldn't load \"${result.name}\" from FDDB."
                } else {
                    pendingProduct = result.copy(food = food)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == resolveRequest) {
                    databaseError = "Couldn't load \"${result.name}\". Check your connection."
                }
            } finally {
                if (request == resolveRequest) resolving = null
            }
        }
    }

    fun lookupBarcode(code: String) {
        val clean = code.trim()
        if (clean.isEmpty()) return
        val request = ++lookupRequest
        scanLoading = true
        scanError = null
        scope.launch {
            try {
                val product = onLookupBarcode(clean)
                if (request != lookupRequest) return@launch
                if (product == null) {
                    scanError = "No product for barcode $clean in Open Food Facts or FDDB."
                } else {
                    pendingProduct = product
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == lookupRequest) {
                    scanError = "Lookup failed. Check your connection and the barcode."
                }
            } finally {
                if (request == lookupRequest) scanLoading = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (customMode) "New food" else "Add food",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                if (mode == AddMode.MY_FOODS && pendingProduct == null) {
                    TextButton(
                        onClick = {
                            customMode = !customMode
                            selectedFoodId = null
                        },
                    ) {
                        Text(if (customMode) "Pick from list" else "New food")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            val pendingSearch = pendingProduct
            val pendingFood = pendingSearch?.food
            val pendingSource = pendingSearch?.source
            when {
                customMode -> CustomFoodForm(
                    meal = meal,
                    onMealChange = { mealName = it.name },
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    onSubmit = onAddCustom,
                )

                pendingFood != null && pendingSource != null -> SelectedFoodEditor(
                    food = pendingFood,
                    subtitle = "${pendingSource.label} · per ${pendingFood.servingLabel}",
                    meal = meal,
                    onMealChange = { mealName = it.name },
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    onBack = {
                        pendingProduct = null
                        databaseError = null
                        scanError = null
                    },
                    onAdd = { onAddProduct(pendingFood, meal, quantity) },
                )

                selectedFood != null -> SelectedFoodEditor(
                    food = selectedFood,
                    subtitle = "Saved food",
                    meal = meal,
                    onMealChange = { mealName = it.name },
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    onBack = { selectedFoodId = null },
                    onAdd = { onAdd(selectedFood.id, meal, quantity) },
                )

                else -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AddMode.entries.forEach { option ->
                            FilterChip(
                                selected = mode == option,
                                onClick = {
                                    modeName = option.name
                                    onModeSelected(option.name)
                                    cancelPendingPicks()
                                },
                                label = { Text(option.label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    when (mode) {
                        AddMode.MY_FOODS -> LocalFoodPicker(
                            query = localQuery,
                            onQueryChange = { localQuery = it },
                            foods = filteredFoods,
                            onSelect = { selectedFoodId = it.id },
                            onRemove = onRemoveFood,
                        )

                        AddMode.DATABASES -> DatabaseFoodPicker(
                            query = databaseQuery,
                            onQueryChange = { databaseQuery = it },
                            onSearch = ::searchDatabases,
                            loading = databaseLoading,
                            note = databaseNote,
                            error = databaseError,
                            results = databaseResults,
                            resolving = resolving,
                            onSelect = ::selectSearchResult,
                        )

                        AddMode.SCAN -> ScanPicker(
                            loading = scanLoading,
                            error = scanError,
                            manualBarcode = manualBarcode,
                            onManualBarcodeChange = { manualBarcode = it },
                            onScan = { scanning = true },
                            onLookup = { lookupBarcode(manualBarcode) },
                        )
                    }
                }
            }
        }
    }

    if (scanning) {
        BarcodeScannerDialog(
            onDismiss = { scanning = false },
            onBarcode = { code ->
                scanning = false
                manualBarcode = code
                lookupBarcode(code)
            },
        )
    }
}

@Composable
private fun LocalFoodPicker(
    query: String,
    onQueryChange: (String) -> Unit,
    foods: List<Food>,
    onSelect: (Food) -> Unit,
    onRemove: (Food) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search foods") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
        items(foods, key = { it.id }) { food ->
            FoodRow(
                food = food,
                source = null,
                onClick = { onSelect(food) },
                onRemove = { onRemove(food) },
            )
        }
        if (foods.isEmpty()) {
            item {
                Text(
                    text = if (query.isBlank()) {
                        "Nothing saved yet. Import a product from \"Databases\" or tap \"New food\"."
                    } else {
                        "No foods match \"$query\""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun DatabaseFoodPicker(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    loading: Boolean,
    note: String?,
    error: String?,
    results: List<FoodSearchResult>,
    resolving: String?,
    onSelect: (FoodSearchResult) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search BLS, FDDB or Open Food Facts") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { if (query.isNotBlank() && !loading) onSearch() },
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onSearch,
        enabled = query.isNotBlank() && !loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Search databases")
    }
    Spacer(Modifier.height(8.dp))
    if (loading) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(10.dp))
            Text("Looking up…", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (error != null) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    if (note != null) {
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    if (results.isNotEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
            items(
                results,
                key = { "${it.source}-${it.name}-${it.food?.calories ?: 0}" },
            ) { result ->
                if (result.food != null) {
                    FoodRow(
                        food = result.food,
                        source = result.source,
                        onClick = { onSelect(result) },
                    )
                } else {
                    PendingFoodRow(
                        result = result,
                        loading = resolving == result.reference,
                        onClick = { onSelect(result) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingFoodRow(result: FoodSearchResult, loading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !loading, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${result.source.label} · tap to load nutrition",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun FoodRow(
    food: Food,
    source: FoodSource?,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                food.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    if (source != null) append(source.label).append(" · ")
                    append(food.servingLabel)
                    append(" · P ").append(food.proteinGrams.gramsText())
                    append(" · C ").append(food.carbsGrams.gramsText())
                    append(" · F ").append(food.fatGrams.gramsText())
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("${food.calories} kcal", style = MaterialTheme.typography.labelLarge)
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove ${food.name} from My foods",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScanPicker(
    loading: Boolean,
    error: String?,
    manualBarcode: String,
    onManualBarcodeChange: (String) -> Unit,
    onScan: () -> Unit,
    onLookup: () -> Unit,
) {
    Text(
        text = "Point the camera at a product barcode, or type the digits printed under it.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
        Text("Scan with camera")
    }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = manualBarcode,
        onValueChange = { onManualBarcodeChange(it.filter(Char::isDigit)) },
        label = { Text("Barcode") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onLookup,
        enabled = manualBarcode.isNotBlank() && !loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Look up barcode")
    }
    if (loading) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(10.dp))
            Text("Looking up…", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (error != null) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun SelectedFoodEditor(
    food: Food,
    subtitle: String,
    meal: Meal,
    onMealChange: (Meal) -> Unit,
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(food.name, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "$subtitle · ${food.calories} kcal per ${food.servingLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text("Meal", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        MealChips(selected = meal, onSelect = onMealChange)
        Spacer(Modifier.height(16.dp))
        ServingInput(
            servingLabel = food.servingLabel,
            quantity = quantity,
            onQuantityChange = onQuantityChange,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${(food.calories * quantity).kcalText()} kcal · " +
                "P ${(food.proteinGrams * quantity).gramsText()} · " +
                "C ${(food.carbsGrams * quantity).gramsText()} · " +
                "F ${(food.fatGrams * quantity).gramsText()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Add") }
        }
    }
}

@Composable
private fun CustomFoodForm(
    meal: Meal,
    onMealChange: (Meal) -> Unit,
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    onSubmit: (
        name: String,
        servingLabel: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        meal: Meal,
        quantity: Double,
    ) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var servingLabel by rememberSaveable { mutableStateOf("100 g") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = servingLabel,
            onValueChange = { servingLabel = it },
            label = { Text("Serving, e.g. 100 g") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = calories,
            onValueChange = { value -> calories = value.filter(Char::isDigit) },
            label = { Text("Calories") },
            suffix = { Text("kcal") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroField("Protein", protein, { protein = it }, Modifier.weight(1f))
            MacroField("Carbs", carbs, { carbs = it }, Modifier.weight(1f))
            MacroField("Fat", fat, { fat = it }, Modifier.weight(1f))
        }
        Text("Meal", style = MaterialTheme.typography.labelLarge)
        MealChips(selected = meal, onSelect = onMealChange)
        ServingInput(
            servingLabel = servingLabel,
            quantity = quantity,
            onQuantityChange = onQuantityChange,
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                val kcal = calories.toIntOrNull() ?: return@Button
                onSubmit(
                    name.trim(),
                    servingLabel.trim().ifBlank { "1 serving" },
                    kcal,
                    protein.toDecimalOrNull() ?: 0.0,
                    carbs.toDecimalOrNull() ?: 0.0,
                    fat.toDecimalOrNull() ?: 0.0,
                    meal,
                    quantity,
                )
            },
            enabled = name.isNotBlank() && (calories.toIntOrNull() ?: -1) >= 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add food")
        }
    }
}

@Composable
private fun MacroField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.filter { it.isDigit() || it == '.' || it == ',' })
        },
        label = { Text(label) },
        suffix = { Text("g") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun MealChips(selected: Meal, onSelect: (Meal) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Meal.entries.forEach { meal ->
            FilterChip(
                selected = selected == meal,
                onClick = { onSelect(meal) },
                label = { Text(meal.label) },
            )
        }
    }
}
