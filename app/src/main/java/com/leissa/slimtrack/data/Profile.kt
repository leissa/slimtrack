package com.leissa.slimtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UnitSystem(val label: String) {
    METRIC("Metric (kg, cm)"),
    IMPERIAL("Imperial (lb, ft/in)"),
}

enum class Sex(val label: String, val calorieFloor: Int) {
    MALE("Male", 1500),
    FEMALE("Female", 1200),
}

enum class ActivityLevel(val label: String, val factor: Double, val hint: String) {
    SEDENTARY("Sedentary", 1.2, "Desk job, little exercise"),
    LIGHT("Light", 1.375, "Exercise 1-3 days a week"),
    MODERATE("Moderate", 1.55, "Exercise 3-5 days a week"),
    ACTIVE("Active", 1.725, "Hard exercise 6-7 days a week"),
    VERY_ACTIVE("Very active", 1.9, "Physical job or two workouts a day"),
}

/**
 * Weekly loss rates use the common approximation of 7700 kcal per kg of body fat,
 * so a tier's daily deficit is [kgPerWeek] * 7700 / 7.
 */
enum class DietTier(
    val label: String,
    val kgPerWeek: Double,
    val deficitKcal: Int,
    val description: String,
) {
    SLOW("Slow", 0.25, 275, "Gentle and easy to sustain"),
    MEDIUM("Medium", 0.5, 550, "Balanced pace, the usual recommendation"),
    FAST("Fast", 0.75, 825, "Aggressive, hardest to stick to"),
}

/** The user's body data and preferences. There is exactly one row with id = 1. */
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val sex: Sex = Sex.MALE,
    val ageYears: Int = 30,
    val heightCm: Double = 175.0,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val dietTier: DietTier = DietTier.MEDIUM,
    val targetWeightKg: Double = 75.0,
    val isConfigured: Boolean = false,
)
