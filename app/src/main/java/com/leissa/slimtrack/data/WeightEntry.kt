package com.leissa.slimtrack.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One weight measurement per day (a new entry for the same day replaces the old one). */
@Entity(
    tableName = "weight_entries",
    indices = [Index(value = ["epochDay"], unique = true)],
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val weightKg: Double,
)
