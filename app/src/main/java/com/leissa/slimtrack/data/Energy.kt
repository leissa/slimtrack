package com.leissa.slimtrack.data

import kotlin.math.roundToInt

/** Daily burn per day, used by the history screen. */
data class DayBurn(val epochDay: Long, val calories: Double)

object Energy {

    /** Mifflin-St Jeor. */
    fun bmr(profile: Profile, weightKg: Double): Double {
        val base = 10.0 * weightKg + 6.25 * profile.heightCm - 5.0 * profile.ageYears
        return base + if (profile.sex == Sex.MALE) 5.0 else -161.0
    }

    fun maintenance(profile: Profile, weightKg: Double): Double =
        bmr(profile, weightKg) * profile.activityLevel.factor

    fun bmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        val meters = heightCm / 100.0
        return weightKg / (meters * meters)
    }

    fun bmiCategory(bmi: Double): String = when {
        bmi <= 0.0 -> "Unknown"
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }

    /** The calorie target for the chosen tier, never below the safety floor. */
    fun suggestedCalories(profile: Profile, weightKg: Double): Int {
        val target = maintenance(profile, weightKg) - profile.dietTier.deficitKcal
        return target.roundToInt().coerceAtLeast(profile.sex.calorieFloor)
    }

    fun isFloorApplied(profile: Profile, weightKg: Double): Boolean {
        val target = maintenance(profile, weightKg) - profile.dietTier.deficitKcal
        return target < profile.sex.calorieFloor
    }

    /**
     * Weeks to the target weight at the *effective* deficit, i.e. after the safety
     * floor has capped intake, so the estimate matches what the user will actually
     * lose. Returns null when the current intake allows no deficit at all.
     */
    fun weeksToTarget(profile: Profile, currentKg: Double): Double? {
        val delta = currentKg - profile.targetWeightKg
        if (delta <= 0.0) return 0.0
        val deficit = maintenance(profile, currentKg) - suggestedCalories(profile, currentKg)
        if (deficit <= 0.0) return null
        return delta * 7700.0 / (deficit * 7.0)
    }

    /** Calories burned for [minutes] of an activity at [weightKg]. */
    fun exerciseCalories(met: Double, weightKg: Double, minutes: Int): Double =
        met * weightKg * (minutes / 60.0)
}
