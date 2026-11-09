package com.leissa.slimtrack.util

/**
 * A serving label that expresses a plain measurable amount, such as "100 g" or
 * "250 ml". Labels like "1 medium" or "1 slice" cannot be converted and return
 * `null` from [parseServingMeasure].
 */
data class ServingMeasure(val amount: Double, val unit: String) {

    /** The amount for [quantity] servings, e.g. 0.05 × 100 g = 5 g. */
    fun amountFor(quantity: Double): Double = amount * quantity

    /** The number of servings that corresponds to [amount] of this measure. */
    fun servingsFor(amount: Double): Double = amount / this.amount
}

private val MEASURE = Regex(
    """^\s*(\d+(?:[.,]\d+)?)\s*(kg|g|ml|l)\s*$""",
    RegexOption.IGNORE_CASE,
)

/** Reads "100 g", "250ml" and "0,5 l"; plain portion labels yield `null`. */
fun parseServingMeasure(label: String): ServingMeasure? {
    val match = MEASURE.find(label) ?: return null
    val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    if (amount <= 0.0) return null
    return ServingMeasure(amount = amount, unit = match.groupValues[2].lowercase())
}

/**
 * The logged amount in the serving's own unit, e.g. "5 g" for 0.05 servings of
 * a "100 g" food. Returns `null` for portion labels such as "1 medium".
 */
fun servingAmountText(label: String, quantity: Double): String? {
    val measure = parseServingMeasure(label) ?: return null
    return "${measure.amountFor(quantity).quantityText()} ${measure.unit}"
}
