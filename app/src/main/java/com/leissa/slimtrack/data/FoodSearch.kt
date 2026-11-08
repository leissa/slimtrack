package com.leissa.slimtrack.data

/** Where a search hit came from, shown next to each result. */
enum class FoodSource(val label: String) {
    BLS("BLS"),
    FDDB("FDDB"),
    OPEN_FOOD_FACTS("Open Food Facts"),
}

/**
 * One search hit. BLS and Open Food Facts hits carry their [Food] immediately;
 * FDDB hits only carry a [reference] because nutrition is read from the product
 * page when the user selects one.
 */
data class FoodSearchResult(
    val name: String,
    val source: FoodSource,
    val food: Food? = null,
    val reference: String? = null,
)

data class FoodSearchOutcome(
    val results: List<FoodSearchResult>,
    /** True when both online sources failed and only the offline BLS results are shown. */
    val onlineUnavailable: Boolean = false,
)
