package com.leissa.slimtrack.util

import com.leissa.slimtrack.data.UnitSystem
import java.util.Locale
import kotlin.math.roundToInt

private const val POUNDS_PER_KG = 2.2046226218
private const val CM_PER_INCH = 2.54

fun kgToPounds(kg: Double): Double = kg * POUNDS_PER_KG
fun poundsToKg(pounds: Double): Double = pounds / POUNDS_PER_KG
fun cmToInches(cm: Double): Double = cm / CM_PER_INCH
fun inchesToCm(inches: Double): Double = inches * CM_PER_INCH

fun weightUnitLabel(unit: UnitSystem): String =
    if (unit == UnitSystem.IMPERIAL) "lb" else "kg"

fun displayWeight(kg: Double, unit: UnitSystem, decimals: Int = 1): String = when (unit) {
    UnitSystem.METRIC -> String.format(Locale.getDefault(), "%.${decimals}f", kg)
    UnitSystem.IMPERIAL -> String.format(Locale.getDefault(), "%.${decimals}f", kgToPounds(kg))
}

fun displayWeightWithUnit(kg: Double, unit: UnitSystem): String =
    "${displayWeight(kg, unit)} ${weightUnitLabel(unit)}"

/** Parses a user-entered weight in the given unit and returns kilograms. */
fun parseWeightToKg(input: String, unit: UnitSystem): Double? {
    val value = input.toDecimalOrNull() ?: return null
    if (value <= 0.0 || value > 700.0) return null
    return if (unit == UnitSystem.IMPERIAL) poundsToKg(value) else value
}

fun displayHeight(cm: Double, unit: UnitSystem): String = when (unit) {
    UnitSystem.METRIC -> "${cm.roundToInt()} cm"
    UnitSystem.IMPERIAL -> {
        val totalInches = cmToInches(cm).roundToInt()
        "${totalInches / 12} ft ${totalInches % 12} in"
    }
}
