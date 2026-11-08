package com.leissa.slimtrack.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyTest {

    private val male = Profile(
        sex = Sex.MALE,
        ageYears = 30,
        heightCm = 180.0,
        activityLevel = ActivityLevel.LIGHT,
        dietTier = DietTier.MEDIUM,
        targetWeightKg = 75.0,
    )

    private val female = male.copy(sex = Sex.FEMALE, heightCm = 165.0)

    @Test
    fun `bmr uses Mifflin-St Jeor`() {
        assertEquals(1780.0, Energy.bmr(male, 80.0), 0.001)
        // 10*80 + 6.25*165 - 5*30 - 161
        assertEquals(1520.25, Energy.bmr(female, 80.0), 0.001)
    }

    @Test
    fun `maintenance multiplies bmr by the activity factor`() {
        assertEquals(1780.0 * 1.375, Energy.maintenance(male, 80.0), 0.001)
    }

    @Test
    fun `bmi converts height to metres`() {
        assertEquals(22.857, Energy.bmi(70.0, 175.0), 0.001)
        assertEquals(0.0, Energy.bmi(70.0, 0.0), 0.0)
    }

    @Test
    fun `suggested calories apply the safety floor`() {
        val tiny = female.copy(ageYears = 80, heightCm = 150.0, activityLevel = ActivityLevel.SEDENTARY)
        assertEquals(1200, Energy.suggestedCalories(tiny, 45.0))
        assertTrue(Energy.isFloorApplied(tiny, 45.0))
        assertFalse(Energy.isFloorApplied(male, 80.0))
    }

    @Test
    fun `weeks to target use the effective deficit`() {
        val weeks = Energy.weeksToTarget(male, 80.0)
        val deficit = Energy.maintenance(male, 80.0) - Energy.suggestedCalories(male, 80.0)
        assertEquals(5.0 * 7700.0 / (deficit * 7.0), weeks!!, 0.001)
    }

    @Test
    fun `weeks to target are zero once the target is reached`() {
        assertEquals(0.0, Energy.weeksToTarget(male, 75.0)!!, 0.0)
        assertEquals(0.0, Energy.weeksToTarget(male, 70.0)!!, 0.0)
    }

    @Test
    fun `weeks to target are unknown when the floor allows no deficit`() {
        val tiny = female.copy(
            ageYears = 100,
            heightCm = 100.0,
            activityLevel = ActivityLevel.SEDENTARY,
            targetWeightKg = 20.0,
        )
        assertNull(Energy.weeksToTarget(tiny, 21.0))
    }

    @Test
    fun `exercise burn scales with met, weight and time`() {
        assertEquals(4.0 * 80.0, Energy.exerciseCalories(4.0, 80.0, 60), 0.001)
        assertEquals(4.0 * 80.0 * 0.5, Energy.exerciseCalories(4.0, 80.0, 30), 0.001)
    }
}
