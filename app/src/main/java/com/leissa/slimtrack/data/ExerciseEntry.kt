package com.leissa.slimtrack.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** MET values from the Compendium of Physical Activities (moderate effort). */
enum class ExerciseType(val label: String, val met: Double) {
    WALKING("Walking (5 km/h)", 3.5),
    HIKING("Hiking", 6.0),
    RUNNING_EASY("Running (8 km/h)", 8.3),
    RUNNING_FAST("Running (12 km/h)", 11.8),
    CYCLING_EASY("Cycling, leisure", 4.0),
    CYCLING_MODERATE("Cycling, moderate", 8.0),
    EBIKE("E-bike", 4.5),
    SWIMMING("Swimming", 6.0),
    ROWING("Rowing machine", 7.0),
    ELLIPTICAL("Elliptical", 5.0),
    STRENGTH("Strength training", 5.0),
    YOGA("Yoga", 2.5),
    FOOTBALL("Football", 7.0),
    TENNIS("Tennis", 7.3),
    DANCING("Dancing", 5.0),
    OTHER("Other", 4.0),
}

@Entity(
    tableName = "exercise_entries",
    indices = [Index("epochDay")],
)
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val activity: ExerciseType,
    val minutes: Int,
    /** Stored at insert time so historical burns do not change when weight changes. */
    val caloriesBurned: Double,
)
