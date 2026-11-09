package com.leissa.slimtrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE name = :name AND calories = :calories LIMIT 1")
    suspend fun findByNameAndCalories(name: String, calories: Int): Food?

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert
    suspend fun insert(food: Food): Long

    @Insert
    suspend fun insertAll(foods: List<Food>)

    @Delete
    suspend fun delete(food: Food)
}

@Dao
interface DiaryDao {
    @Transaction
    @Query("SELECT * FROM entries WHERE dateEpochDay = :epochDay ORDER BY id")
    fun observeForDay(epochDay: Long): Flow<List<EntryWithFood>>

    @Insert
    suspend fun insert(entry: DiaryEntry)

    @Query("UPDATE entries SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Double)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query(
        """
        SELECT e.dateEpochDay AS epochDay,
               COALESCE(SUM(f.calories * e.quantity), 0.0) AS calories,
               COALESCE(SUM(f.proteinGrams * e.quantity), 0.0) AS proteinGrams,
               COALESCE(SUM(f.carbsGrams * e.quantity), 0.0) AS carbsGrams,
               COALESCE(SUM(f.fatGrams * e.quantity), 0.0) AS fatGrams
        FROM entries e
        INNER JOIN foods f ON f.id = e.foodId
        WHERE e.dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY e.dateEpochDay
        ORDER BY e.dateEpochDay
        """
    )
    fun observeDailyTotals(fromEpochDay: Long, toEpochDay: Long): Flow<List<DayTotals>>
}

@Dao
interface GoalsDao {
    @Query("SELECT * FROM goals WHERE id = 1")
    fun observe(): Flow<Goals?>

    @Query("SELECT * FROM goals WHERE id = 1")
    suspend fun get(): Goals?

    @Upsert
    suspend fun upsert(goals: Goals)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<Profile?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): Profile?

    @Upsert
    suspend fun upsert(profile: Profile)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY epochDay")
    fun observeAll(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries ORDER BY epochDay DESC LIMIT 1")
    suspend fun latest(): WeightEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntry)

    @Delete
    suspend fun delete(entry: WeightEntry)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_entries WHERE epochDay = :epochDay ORDER BY id")
    fun observeForDay(epochDay: Long): Flow<List<ExerciseEntry>>

    @Insert
    suspend fun insert(entry: ExerciseEntry)

    @Delete
    suspend fun delete(entry: ExerciseEntry)

    @Query(
        """
        SELECT epochDay AS epochDay,
               COALESCE(SUM(caloriesBurned), 0.0) AS calories
        FROM exercise_entries
        WHERE epochDay BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY epochDay
        ORDER BY epochDay
        """
    )
    fun observeDailyBurn(fromEpochDay: Long, toEpochDay: Long): Flow<List<DayBurn>>
}
