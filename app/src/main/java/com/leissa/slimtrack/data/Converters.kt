package com.leissa.slimtrack.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMeal(meal: Meal): String = meal.name

    @TypeConverter
    fun toMeal(value: String): Meal = Meal.valueOf(value)

    @TypeConverter
    fun fromUnitSystem(value: UnitSystem): String = value.name

    @TypeConverter
    fun toUnitSystem(value: String): UnitSystem = UnitSystem.valueOf(value)

    @TypeConverter
    fun fromSex(value: Sex): String = value.name

    @TypeConverter
    fun toSex(value: String): Sex = Sex.valueOf(value)

    @TypeConverter
    fun fromActivityLevel(value: ActivityLevel): String = value.name

    @TypeConverter
    fun toActivityLevel(value: String): ActivityLevel = ActivityLevel.valueOf(value)

    @TypeConverter
    fun fromDietTier(value: DietTier): String = value.name

    @TypeConverter
    fun toDietTier(value: String): DietTier = DietTier.valueOf(value)

    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)
}
