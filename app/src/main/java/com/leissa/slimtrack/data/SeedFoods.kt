package com.leissa.slimtrack.data

/** A starter food list so the app is useful on first launch. */
object SeedFoods {
    private fun food(
        name: String,
        servingLabel: String,
        calories: Int,
        proteinGrams: Double,
        carbsGrams: Double,
        fatGrams: Double,
    ): Food = Food(
        name = name,
        servingLabel = servingLabel,
        calories = calories,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
    )

    val items = listOf(
        // Breakfast
        food("Oatmeal (dry)", "40 g", 150, 5.0, 27.0, 3.0),
        food("Banana", "1 medium", 105, 1.3, 27.0, 0.4),
        food("Boiled egg", "1 egg", 78, 6.3, 0.6, 5.3),
        food("Greek yogurt 0%", "150 g", 87, 15.0, 5.4, 0.6),
        food("Whole-grain bread", "1 slice", 80, 4.0, 14.0, 1.0),
        food("Butter", "1 tsp", 34, 0.0, 0.0, 3.8),
        food("Peanut butter", "1 tbsp", 94, 4.0, 3.0, 8.0),
        food("Coffee with milk", "1 cup", 25, 1.5, 2.5, 1.0),
        food("Orange juice", "250 ml", 112, 1.7, 26.0, 0.5),

        // Mains and protein
        food("Chicken breast", "100 g", 165, 31.0, 0.0, 3.6),
        food("Salmon fillet", "100 g", 208, 20.0, 0.0, 13.0),
        food("Lean beef", "100 g", 217, 26.0, 0.0, 12.0),
        food("Tofu, firm", "100 g", 76, 8.0, 1.9, 4.8),
        food("Lentils, cooked", "100 g", 116, 9.0, 20.0, 0.4),
        food("Tuna, canned in water", "100 g", 116, 26.0, 0.0, 1.0),
        food("Protein shake", "1 scoop", 120, 24.0, 3.0, 1.5),

        // Carbohydrates
        food("White rice, cooked", "100 g", 130, 2.7, 28.0, 0.3),
        food("Brown rice, cooked", "100 g", 123, 2.7, 26.0, 1.0),
        food("Pasta, cooked", "100 g", 158, 5.8, 31.0, 0.9),
        food("Potato, boiled", "100 g", 77, 2.0, 17.0, 0.1),
        food("Sweet potato", "100 g", 86, 1.6, 20.0, 0.1),
        food("Quinoa, cooked", "100 g", 120, 4.4, 21.0, 1.9),
        food("Bread roll", "1 roll", 170, 5.5, 33.0, 1.5),

        // Vegetables, fruit and fats
        food("Broccoli", "100 g", 34, 2.8, 7.0, 0.4),
        food("Mixed salad", "100 g", 20, 1.2, 3.0, 0.2),
        food("Avocado", "100 g", 160, 2.0, 9.0, 15.0),
        food("Apple", "1 medium", 95, 0.5, 25.0, 0.3),
        food("Blueberries", "100 g", 57, 0.7, 14.0, 0.3),
        food("Almonds", "28 g", 164, 6.0, 6.0, 14.0),
        food("Olive oil", "1 tbsp", 119, 0.0, 0.0, 13.5),

        // Snacks and drinks
        food("Dark chocolate", "20 g", 110, 1.5, 12.0, 6.0),
        food("Pizza slice", "1 slice", 285, 12.0, 36.0, 10.0),
        food("Ice cream", "100 g", 207, 3.5, 24.0, 11.0),
        food("Milk, 1.5%", "250 ml", 122, 8.0, 12.0, 4.8),
        food("Beer", "330 ml", 140, 1.6, 11.0, 0.0),
    )
}
