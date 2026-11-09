package com.leissa.slimtrack.util

import com.leissa.slimtrack.data.Meal
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Built per call so a locale change while the app runs is picked up.
fun LocalDate.dayLabel(): String = when (this) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
}

fun LocalDate.shortDayLabel(): String =
    format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

/** "1,240" in English, "1.240" in German. */
fun Double.kcalText(): String = String.format(Locale.getDefault(), "%,.0f", this)

fun Double.gramsText(): String = String.format(Locale.getDefault(), "%.0f g", this)

/**
 * Prints servings and amounts without unnecessary decimals. Two decimals are
 * kept so small portions stay possible, e.g. 0.05 servings of oil.
 */
fun Double.quantityText(): String =
    if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", this)
            .trimEnd('0')
            .trimEnd('.', ',')
    }

/** Accepts both "12.5" and "12,5". */
fun String.toDecimalOrNull(): Double? = replace(',', '.').trim().toDoubleOrNull()

fun String.toIntOrNullSafe(): Int? = trim().toIntOrNull()

fun defaultMealForNow(): Meal = when (LocalTime.now().hour) {
    in 0..9 -> Meal.BREAKFAST
    in 10..14 -> Meal.LUNCH
    in 15..20 -> Meal.DINNER
    else -> Meal.SNACK
}
