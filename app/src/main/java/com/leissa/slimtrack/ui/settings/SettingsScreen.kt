@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.leissa.slimtrack.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leissa.slimtrack.data.ActivityLevel
import com.leissa.slimtrack.data.DietTier
import com.leissa.slimtrack.data.Energy
import com.leissa.slimtrack.data.Goals
import com.leissa.slimtrack.data.Profile
import com.leissa.slimtrack.data.Sex
import com.leissa.slimtrack.data.UnitSystem
import com.leissa.slimtrack.util.cmToInches
import com.leissa.slimtrack.util.displayHeight
import com.leissa.slimtrack.util.displayWeight
import com.leissa.slimtrack.util.displayWeightWithUnit
import com.leissa.slimtrack.util.inchesToCm
import com.leissa.slimtrack.util.kcalText
import com.leissa.slimtrack.util.parseWeightToKg
import com.leissa.slimtrack.util.toDecimalOrNull
import com.leissa.slimtrack.util.weightUnitLabel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    val goals = state.goals

    var unitName by rememberSaveable(profile) { mutableStateOf(profile.unitSystem.name) }
    var sexName by rememberSaveable(profile) { mutableStateOf(profile.sex.name) }
    var ageText by rememberSaveable(profile) { mutableStateOf(profile.ageYears.toString()) }
    var activityName by rememberSaveable(profile) { mutableStateOf(profile.activityLevel.name) }
    var tierName by rememberSaveable(profile) { mutableStateOf(profile.dietTier.name) }
    var unitTick by rememberSaveable { mutableIntStateOf(0) }

    val unit = UnitSystem.valueOf(unitName)
    val sex = Sex.valueOf(sexName)
    val activity = ActivityLevel.valueOf(activityName)
    val tier = DietTier.valueOf(tierName)
    val profileWeightUnit = weightUnitLabel(unit)

    val initialHeight = profile.heightCm
    var heightCmText by rememberSaveable(profile, unitTick) {
        mutableStateOf(
            if (unit == UnitSystem.METRIC) "%.0f".format(Locale.getDefault(), initialHeight)
            else "%.0f".format(Locale.getDefault(), (cmToInches(initialHeight).roundToInt() / 12).toDouble())
        )
    }
    var heightInText by rememberSaveable(profile, unitTick) {
        mutableStateOf(
            "%.0f".format(Locale.getDefault(), cmToInches(initialHeight).roundToInt() % 12 / 1.0)
        )
    }
    var targetWeightText by rememberSaveable(profile, unitTick) {
        mutableStateOf(displayWeight(profile.targetWeightKg, unit))
    }
    var targetBmiText by rememberSaveable(profile, unitTick) {
        mutableStateOf(
            String.format(
                Locale.getDefault(),
                "%.1f",
                Energy.bmi(profile.targetWeightKg, heightRoundedForUnit(profile.heightCm, unit)),
            )
        )
    }

    var goalCaloriesText by rememberSaveable(goals) { mutableStateOf(goals.calories.toString()) }
    var goalProteinText by rememberSaveable(goals) { mutableStateOf(goals.proteinGrams.toString()) }
    var goalCarbsText by rememberSaveable(goals) { mutableStateOf(goals.carbsGrams.toString()) }
    var goalFatText by rememberSaveable(goals) { mutableStateOf(goals.fatGrams.toString()) }
    var savedMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun parsedHeightCm(): Double? = if (unit == UnitSystem.METRIC) {
        heightCmText.toDecimalOrNull()
    } else {
        val feet = heightCmText.toDecimalOrNull() ?: return null
        val inches = heightInText.toDecimalOrNull() ?: 0.0
        if (feet <= 0.0) null else inchesToCm(feet * 12.0 + inches)
    }

    fun bmiString(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)

    fun syncBmiFromWeight(height: Double) {
        parseWeightToKg(targetWeightText, unit)?.let { kg ->
            targetBmiText = bmiString(Energy.bmi(kg, height))
        }
    }

    fun syncTargetFromBmi(height: Double) {
        val bmi = targetBmiText.toDecimalOrNull() ?: return
        if (bmi <= 0.0 || height <= 0.0) return
        val meters = height / 100.0
        targetWeightText = displayWeight(bmi * meters * meters, unit)
    }

    val heightCm = parsedHeightCm()
    val targetWeightKg = parseWeightToKg(targetWeightText, unit)
    val age = ageText.trim().toIntOrNull()?.coerceIn(10, 120) ?: profile.ageYears

    val workingProfile = profile.copy(
        unitSystem = unit,
        sex = sex,
        ageYears = age,
        heightCm = heightCm ?: profile.heightCm,
        activityLevel = activity,
        dietTier = tier,
        targetWeightKg = targetWeightKg ?: profile.targetWeightKg,
    )

    val weightForEnergy = state.latestWeightKg ?: workingProfile.targetWeightKg
    val maintenance = Energy.maintenance(workingProfile, weightForEnergy)
    val suggestedCalories = Energy.suggestedCalories(workingProfile, weightForEnergy)
    val floorApplied = Energy.isFloorApplied(workingProfile, weightForEnergy)
    val weeksToTarget = Energy.weeksToTarget(
        weightForEnergy,
        workingProfile.targetWeightKg,
        tier,
    )
    val targetBmiValue = Energy.bmi(workingProfile.targetWeightKg, workingProfile.heightCm)

    val canSave = heightCm != null && heightCm > 0 &&
        targetWeightKg != null && goalCaloriesText.toIntOrNull() != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Profile & goals",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Used for BMI, calorie targets and exercise burn.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        state.latestWeightKg?.let { kg ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Current weight",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = displayWeightWithUnit(kg, unit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        SectionTitle("Units")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitSystem.entries.forEach { option ->
                FilterChip(
                    selected = unit == option,
                    onClick = {
                        if (unit != option) {
                            unitName = option.name
                            unitTick += 1
                        }
                    },
                    label = { Text(option.label) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Body")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sex.entries.forEach { option ->
                FilterChip(
                    selected = sex == option,
                    onClick = { sexName = option.name },
                    label = { Text(option.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it.filter(Char::isDigit).take(3) },
                label = { Text("Age") },
                suffix = { Text("yr") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            if (unit == UnitSystem.METRIC) {
                OutlinedTextField(
                    value = heightCmText,
                    onValueChange = { input ->
                        heightCmText = input.filter { it.isDigit() || it == '.' || it == ',' }
                        parsedHeightCm()?.let { syncBmiFromWeight(it) }
                    },
                    label = { Text("Height") },
                    suffix = { Text("cm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            } else {
                OutlinedTextField(
                    value = heightCmText,
                    onValueChange = { input ->
                        heightCmText = input.filter(Char::isDigit).take(1)
                        parsedHeightCm()?.let { syncBmiFromWeight(it) }
                    },
                    label = { Text("Height") },
                    suffix = { Text("ft") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = heightInText,
                    onValueChange = { input ->
                        heightInText = input.filter(Char::isDigit).take(2)
                        parsedHeightCm()?.let { syncBmiFromWeight(it) }
                    },
                    label = { Text("Height") },
                    suffix = { Text("in") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (heightCm != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "= ${displayHeight(heightCm, unit)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Activity")
        Text(
            text = activity.hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityLevel.entries.forEach { option ->
                FilterChip(
                    selected = activity == option,
                    onClick = { activityName = option.name },
                    label = { Text(option.label) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Diet tier")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DietTier.entries.forEach { option ->
                FilterChip(
                    selected = tier == option,
                    onClick = { tierName = option.name },
                    label = {
                        Text(
                            "${option.label} · " +
                                "${displayWeight(option.kgPerWeek, unit, 2)} $profileWeightUnit/week"
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = tier.description + ". Deficit ${tier.deficitKcal} kcal/day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle("Target")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = targetWeightText,
                onValueChange = { input ->
                    targetWeightText = input.filter { it.isDigit() || it == '.' || it == ',' }
                    heightCm?.let { syncBmiFromWeight(it) }
                },
                label = { Text("Target weight") },
                suffix = { Text(profileWeightUnit) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = targetBmiText,
                onValueChange = { input ->
                    targetBmiText = input.filter { it.isDigit() || it == '.' || it == ',' }
                    heightCm?.let { syncTargetFromBmi(it) }
                },
                label = { Text("Target BMI") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Target BMI %.1f is in the ${Energy.bmiCategory(targetBmiValue).lowercase()} range."
                .format(Locale.getDefault(), targetBmiValue),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle("Recommendation")
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                InfoRow("Maintenance", "${maintenance.kcalText()} kcal/day")
                InfoRow("Tier deficit", "−${tier.deficitKcal} kcal/day")
                InfoRow("Suggested intake", "${suggestedCalories.toDouble().kcalText()} kcal/day")
                InfoRow(
                    "Expected loss",
                    "${displayWeight(tier.kgPerWeek, unit, 2)} $profileWeightUnit/week",
                )
                InfoRow(
                    "Time to target",
                    if (weeksToTarget <= 0.0) "Reached"
                    else "%.0f weeks".format(Locale.getDefault(), weeksToTarget),
                )
                if (state.latestWeightKg == null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Log a weight on the Weight tab for a personal estimate; " +
                            "the target weight is used for now.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (floorApplied) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "The safety floor of ${sex.calorieFloor} kcal is applied. " +
                            "Choose a slower tier if that feels too low.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        goalCaloriesText = suggestedCalories.toString()
                        goalProteinText = (suggestedCalories * 0.25 / 4.0).roundToInt().toString()
                        goalCarbsText = (suggestedCalories * 0.45 / 4.0).roundToInt().toString()
                        goalFatText = (suggestedCalories * 0.30 / 9.0).roundToInt().toString()
                        savedMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Use ${suggestedCalories.toDouble().kcalText()} kcal as goal")
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Daily goals")
        OutlinedTextField(
            value = goalCaloriesText,
            onValueChange = { goalCaloriesText = it.filter(Char::isDigit).take(4) },
            label = { Text("Calories") },
            suffix = { Text("kcal") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalField("Protein", goalProteinText, { goalProteinText = it }, Modifier.weight(1f))
            GoalField("Carbs", goalCarbsText, { goalCarbsText = it }, Modifier.weight(1f))
            GoalField("Fat", goalFatText, { goalFatText = it }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val parsedGoals = Goals(
                    calories = goalCaloriesText.toIntOrNull() ?: goals.calories,
                    proteinGrams = goalProteinText.toIntOrNull() ?: goals.proteinGrams,
                    carbsGrams = goalCarbsText.toIntOrNull() ?: goals.carbsGrams,
                    fatGrams = goalFatText.toIntOrNull() ?: goals.fatGrams,
                )
                viewModel.save(
                    workingProfile.copy(
                        targetWeightKg = targetWeightKg ?: profile.targetWeightKg,
                        heightCm = heightCm ?: profile.heightCm,
                    ),
                    parsedGoals,
                )
                savedMessage = "Saved"
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save profile & goals")
        }
        if (savedMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = savedMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(32.dp))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Your data stays on this device",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "The diary, weights and profile live in a local database. " +
                        "Online searches send only your search terms to FDDB and Open Food Facts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "German foods: Bundeslebensmittelschlüssel 4.0, " +
                        "© Max Rubner-Institut (CC BY 4.0). Branded products: Open Food Facts (ODbL) " +
                        "and FDDB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Height as the unit fields actually represent it, so BMI captions match the inputs. */
private fun heightRoundedForUnit(heightCm: Double, unit: UnitSystem): Double =
    if (unit == UnitSystem.METRIC) {
        heightCm
    } else {
        inchesToCm(cmToInches(heightCm).roundToInt().toDouble())
    }

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GoalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        suffix = { Text("g") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
