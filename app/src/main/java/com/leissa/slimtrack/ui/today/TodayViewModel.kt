package com.leissa.slimtrack.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leissa.slimtrack.data.AppContainer
import com.leissa.slimtrack.data.DiaryEntry
import com.leissa.slimtrack.data.Energy
import com.leissa.slimtrack.data.EntryWithFood
import com.leissa.slimtrack.data.ExerciseEntry
import com.leissa.slimtrack.data.ExerciseType
import com.leissa.slimtrack.data.Food
import com.leissa.slimtrack.data.FoodSearchOutcome
import com.leissa.slimtrack.data.FoodSearchResult
import com.leissa.slimtrack.data.FoodSource
import com.leissa.slimtrack.data.Goals
import com.leissa.slimtrack.data.Meal
import com.leissa.slimtrack.data.Profile
import com.leissa.slimtrack.util.trackDayChanges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val entries: List<EntryWithFood> = emptyList(),
    val goals: Goals = Goals(),
    val foods: List<Food> = emptyList(),
    val exercises: List<ExerciseEntry> = emptyList(),
    val profile: Profile = Profile(),
    val latestWeightKg: Double? = null,
) {
    val calories: Double = entries.sumOf { it.calories }
    val protein: Double = entries.sumOf { it.protein }
    val carbs: Double = entries.sumOf { it.carbs }
    val fat: Double = entries.sumOf { it.fat }

    val burned: Double = exercises.sumOf { it.caloriesBurned }

    /** Activity raises the budget for the day rather than replacing the goal. */
    val effectiveGoal: Int = goals.calories + burned.toInt()

    val remaining: Double = effectiveGoal - calories

    val isToday: Boolean get() = date == today

    fun entriesFor(meal: Meal): List<EntryWithFood> = entries.filter { it.entry.meal == meal }

    fun caloriesFor(meal: Meal): Double = entriesFor(meal).sumOf { it.calories }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val container: AppContainer) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    init {
        viewModelScope.trackDayChanges { refreshToday() }
    }

    /**
     * Called on resume and shortly after midnight. If the user was looking at today,
     * follow the new day; a date the user navigated to deliberately is left alone.
     */
    fun refreshToday() {
        val now = LocalDate.now()
        val previous = today.value
        if (now == previous) return
        val wasOnToday = selectedDate.value == previous
        today.value = now
        if (wasOnToday) selectedDate.value = now
    }

    private val dayEntries = selectedDate.flatMapLatest { date ->
        container.diaryDao.observeForDay(date.toEpochDay())
    }

    private val dayExercises = selectedDate.flatMapLatest { date ->
        container.exerciseDao.observeForDay(date.toEpochDay())
    }

    private val dayData = combine(selectedDate, dayEntries, dayExercises) { date, entries, exercises ->
        Triple(date, entries, exercises)
    }

    private val settings = combine(
        container.goalsDao.observe(),
        container.profileDao.observe(),
        container.weightDao.observeLatest(),
    ) { goals, profile, weight ->
        Triple(goals ?: Goals(), profile ?: Profile(), weight?.weightKg)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        dayData,
        settings,
        container.foodDao.observeAll(),
        today,
    ) { (date, entries, exercises), (goals, profile, latestWeight), foods, currentDay ->
        TodayUiState(
            date = date,
            today = currentDay,
            entries = entries,
            goals = goals,
            foods = foods,
            exercises = exercises,
            profile = profile,
            latestWeightKg = latestWeight,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    fun previousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        val next = selectedDate.value.plusDays(1)
        if (!next.isAfter(today.value)) {
            selectedDate.value = next
        }
    }

    fun goToToday() {
        refreshToday()
        selectedDate.value = today.value
    }

    fun addEntry(foodId: Long, meal: Meal, quantity: Double) {
        viewModelScope.launch {
            container.diaryDao.insert(
                DiaryEntry(
                    dateEpochDay = selectedDate.value.toEpochDay(),
                    meal = meal,
                    foodId = foodId,
                    quantity = quantity,
                )
            )
        }
    }

    fun addCustomFood(
        name: String,
        servingLabel: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        meal: Meal,
        quantity: Double,
    ) {
        viewModelScope.launch {
            val foodId = container.foodDao.insert(
                Food(
                    name = name,
                    servingLabel = servingLabel,
                    calories = calories,
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    isCustom = true,
                )
            )
            container.diaryDao.insert(
                DiaryEntry(
                    dateEpochDay = selectedDate.value.toEpochDay(),
                    meal = meal,
                    foodId = foodId,
                    quantity = quantity,
                )
            )
        }
    }

    /** Stores a product from BLS, FDDB, Open Food Facts or a scan, then logs one serving. */
    fun addProduct(food: Food, meal: Meal, quantity: Double) {
        viewModelScope.launch {
            // Products are cached locally, so reuse an identical row instead of piling up copies.
            val existing = container.foodDao.findByNameAndCalories(food.name, food.calories)
            val foodId = existing?.id ?: container.foodDao.insert(food.copy(isCustom = true))
            container.diaryDao.insert(
                DiaryEntry(
                    dateEpochDay = selectedDate.value.toEpochDay(),
                    meal = meal,
                    foodId = foodId,
                    quantity = quantity,
                )
            )
        }
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch { container.diaryDao.delete(entry) }
    }

    fun addExercise(type: ExerciseType, minutes: Int) {
        viewModelScope.launch {
            // Burn is computed once from the latest weight and stored with the entry.
            val weightKg = container.weightDao.latest()?.weightKg ?: DEFAULT_WEIGHT_KG
            container.exerciseDao.insert(
                ExerciseEntry(
                    epochDay = selectedDate.value.toEpochDay(),
                    activity = type,
                    minutes = minutes,
                    caloriesBurned = Energy.exerciseCalories(type.met, weightKg, minutes),
                )
            )
        }
    }

    fun deleteExercise(entry: ExerciseEntry) {
        viewModelScope.launch { container.exerciseDao.delete(entry) }
    }

    /**
     * Searches the offline BLS table and both online databases concurrently.
     * A failing online source must not hide the other results, so failures are
     * collected instead of thrown.
     */
    suspend fun searchFoods(query: String): FoodSearchOutcome = coroutineScope {
        val bls = async { runCatching { container.blsFoods.search(query) } }
        val off = async { runCatching { container.openFoodFacts.search(query) } }
        val fddb = async { runCatching { container.fddb.search(query) } }

        val local = bls.await().getOrDefault(emptyList())
            .map { FoodSearchResult(name = it.name, source = FoodSource.BLS, food = it) }
        val openFoodFacts = off.await()
        val fddbResult = fddb.await()

        val onlineHits = fddbResult.getOrDefault(emptyList())
        val openFoodFactsHits = openFoodFacts.getOrDefault(emptyList())
            .map { FoodSearchResult(name = it.name, source = FoodSource.OPEN_FOOD_FACTS, food = it) }

        FoodSearchOutcome(
            results = interleave(local, onlineHits, openFoodFactsHits),
            onlineUnavailable = openFoodFacts.isFailure && fddbResult.isFailure,
        )
    }

    /** Alternates sources so each one's best matches stay near the top of the list. */
    private fun interleave(vararg groups: List<FoodSearchResult>): List<FoodSearchResult> {
        val longest = groups.maxOfOrNull { it.size } ?: 0
        val merged = ArrayList<FoodSearchResult>(groups.sumOf { it.size })
        for (index in 0 until longest) {
            groups.forEach { group ->
                if (index < group.size) merged += group[index]
            }
        }
        return merged
    }

    suspend fun resolveSearchResult(result: FoodSearchResult): Food? =
        result.food ?: container.fddb.fetchProduct(result)

    suspend fun lookupBarcode(barcode: String): Food? = container.openFoodFacts.fetchByBarcode(barcode)

    companion object {
        private const val DEFAULT_WEIGHT_KG = 75.0

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(container) }
        }
    }
}
