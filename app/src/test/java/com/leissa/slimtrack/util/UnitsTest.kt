package com.leissa.slimtrack.util

import com.leissa.slimtrack.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {

    @Test
    fun `weight conversions round trip`() {
        assertEquals(70.0, poundsToKg(kgToPounds(70.0)), 0.000001)
        assertEquals(154.324, kgToPounds(70.0), 0.001)
    }

    @Test
    fun `height conversions round trip`() {
        assertEquals(175.0, inchesToCm(cmToInches(175.0)), 0.000001)
        assertEquals(2.54, inchesToCm(1.0), 0.0)
    }

    @Test
    fun `metric weight parsing`() {
        assertEquals(82.5, parseWeightToKg("82,5", UnitSystem.METRIC)!!, 0.0)
        assertEquals(82.5, parseWeightToKg("82.5", UnitSystem.METRIC)!!, 0.0)
        assertEquals(70.0, parseWeightToKg("70", UnitSystem.METRIC)!!, 0.0)
        assertNull(parseWeightToKg("0", UnitSystem.METRIC))
        assertNull(parseWeightToKg("700,1", UnitSystem.METRIC))
        assertNull(parseWeightToKg("abc", UnitSystem.METRIC))
        assertNull(parseWeightToKg("", UnitSystem.METRIC))
    }

    @Test
    fun `imperial input is converted to kilograms`() {
        // 154.3 lb is 69.989 kg.
        assertEquals(69.989, parseWeightToKg("154.3", UnitSystem.IMPERIAL)!!, 0.001)
        // The upper bound is checked in the display unit before conversion.
        assertNull(parseWeightToKg("800", UnitSystem.IMPERIAL))
    }

    @Test
    fun `height is displayed as feet and inches`() {
        assertEquals("5 ft 9 in", displayHeight(175.0, UnitSystem.IMPERIAL))
        assertEquals("175 cm", displayHeight(175.0, UnitSystem.METRIC))
    }

    @Test
    fun `decimal input accepts both separators`() {
        assertEquals(12.5, "12,5".toDecimalOrNull()!!, 0.0)
        assertEquals(12.5, " 12.5 ".toDecimalOrNull()!!, 0.0)
        assertNull("twelve".toDecimalOrNull())
    }
}
