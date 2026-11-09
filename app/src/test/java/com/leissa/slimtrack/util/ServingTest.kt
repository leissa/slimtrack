package com.leissa.slimtrack.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServingTest {

    @Test
    fun `measured serving labels are parsed`() {
        assertEquals(ServingMeasure(100.0, "g"), parseServingMeasure("100 g"))
        assertEquals(ServingMeasure(250.0, "ml"), parseServingMeasure("250ml"))
        assertEquals(ServingMeasure(0.5, "l"), parseServingMeasure("0,5 l"))
        assertEquals(ServingMeasure(28.0, "g"), parseServingMeasure(" 28 G "))
    }

    @Test
    fun `portion labels are not treated as measured servings`() {
        assertNull(parseServingMeasure("1 medium"))
        assertNull(parseServingMeasure("1 slice"))
        assertNull(parseServingMeasure("100"))
        assertNull(parseServingMeasure("0 g"))
        assertNull(parseServingMeasure(""))
    }

    @Test
    fun `small amounts of a coarse serving are shown in its unit`() {
        // A teaspoon of oil from a 100 ml serving is 0.05 servings.
        assertEquals("5 g", servingAmountText("100 g", 0.05))
        assertEquals("375 ml", servingAmountText("250 ml", 1.5))
        assertNull(servingAmountText("1 medium", 1.0))
    }
}
