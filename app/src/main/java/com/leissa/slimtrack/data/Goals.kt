package com.leissa.slimtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The single row of daily targets. */
@Entity(tableName = "goals")
data class Goals(
    @PrimaryKey val id: Int = 1,
    val calories: Int = 2000,
    val proteinGrams: Int = 150,
    val carbsGrams: Int = 220,
    val fatGrams: Int = 70,
)
