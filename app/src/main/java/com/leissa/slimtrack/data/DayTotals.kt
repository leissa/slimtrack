package com.leissa.slimtrack.data

/** Aggregated nutrition for one day, used by the history screen. */
data class DayTotals(
    val epochDay: Long,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)
