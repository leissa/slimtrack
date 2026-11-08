package com.leissa.slimtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A food item with its nutrition for one serving.
 *
 * [servingLabel] describes the serving, e.g. "100 g", "1 medium" or "1 slice",
 * so quantities are expressed in servings rather than grams.
 */
@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servingLabel: String,
    val calories: Int,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val isCustom: Boolean = false,
)
