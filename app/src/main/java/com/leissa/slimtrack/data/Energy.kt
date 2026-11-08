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

    fun weeksToTarget(currentKg: Double, targetKg: Double, tier: DietTier): Double {
        val delta = currentKg - targetKg
        return if (delta <= 0.0) 0.0 else delta / tier.kgPerWeek
    }

    /** Calories burned for [minutes] of an activity at [weightKg]. */
    fun exerciseCalories(met: Double, weightKg: Double, minutes: Int): Double =
        met * weightKg * (minutes / 60.0)
}
