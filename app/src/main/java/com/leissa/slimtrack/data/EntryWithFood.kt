package com.leissa.slimtrack.data

import androidx.room.Embedded
import androidx.room.Relation

/** A diary entry together with the food it points at. */
data class EntryWithFood(
    @Embedded val entry: DiaryEntry,
    @Relation(parentColumn = "foodId", entityColumn = "id")
    val food: Food,
) {
    val calories: Double get() = food.calories * entry.quantity
    val protein: Double get() = food.proteinGrams * entry.quantity
    val carbs: Double get() = food.carbsGrams * entry.quantity
    val fat: Double get() = food.fatGrams * entry.quantity
}
