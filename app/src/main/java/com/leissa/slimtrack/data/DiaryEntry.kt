package com.leissa.slimtrack.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One logged food on one day. */
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dateEpochDay"), Index("foodId")],
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val meal: Meal,
    val foodId: Long,
    /** Number of servings. */
    val quantity: Double = 1.0,
)
