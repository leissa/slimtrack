package com.leissa.slimtrack.ui.weight

import com.leissa.slimtrack.data.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightPointsTest {

    private val entries = listOf(
        WeightEntry(id = 1, epochDay = 0, weightKg = 80.0),
        WeightEntry(id = 2, epochDay = 1, weightKg = 79.0),
        WeightEntry(id = 3, epochDay = 2, weightKg = 81.0),
        WeightEntry(id = 4, epochDay = 3, weightKg = 78.0),
        WeightEntry(id = 5, epochDay = 10, weightKg = 77.0),
    )

    @Test
    fun `one point is produced per entry`() {
        val points = weightPoints(entries)
        assertEquals(entries.map { it.epochDay }, points.map { it.epochDay })
        assertEquals(entries.map { it.weightKg }, points.map { it.kg })
    }

    @Test
    fun `three day average covers the logged days in the window`() {
        val points = weightPoints(entries)
        assertEquals((80.0 + 79.0 + 81.0) / 3.0, points[2].avg3, 0.000001)
        // Day 10 has no neighbours logged, so its average is its own weight.
        assertEquals(77.0, points[4].avg3, 0.000001)
    }

    @Test
    fun `seven day average covers the logged days in the window`() {
        val points = weightPoints(entries)
        // Days 0..3 all fall inside the seven-day window of day 3.
        assertEquals((80.0 + 79.0 + 81.0 + 78.0) / 4.0, points[3].avg7, 0.000001)
        // Day 10's window is days 4..10, which only contains day 10.
        assertEquals(77.0, points[4].avg7, 0.000001)
    }
}
