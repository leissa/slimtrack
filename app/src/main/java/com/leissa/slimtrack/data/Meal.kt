package com.leissa.slimtrack.data

/** The meals a diary entry can belong to. Stored by name via [Converters]. */
enum class Meal(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snacks"),
}
