package com.leissa.slimtrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leissa.slimtrack.data.AppContainer
import com.leissa.slimtrack.data.Goals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HistoryDay(
    val date: LocalDate,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val burned: Double = 0.0,
) {
    val net: Double get() = calories - burned
}

private const val DAY_COUNT = 14

class HistoryViewModel(container: AppContainer) : ViewModel() {

    val goals: StateFlow<Goals> = container.goalsDao.observe()
        .map { it ?: Goals() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Goals())

    val days: StateFlow<List<HistoryDay>> = run {
        val today = LocalDate.now()
        val from = today.minusDays((DAY_COUNT - 1).toLong())
        combine(
            container.diaryDao.observeDailyTotals(from.toEpochDay(), today.toEpochDay()),
            container.exerciseDao.observeDailyBurn(from.toEpochDay(), today.toEpochDay()),
        ) { totals, burns ->
            val byDay = totals.associateBy { it.epochDay }
            val burnByDay = burns.associateBy { it.epochDay }
            (0 until DAY_COUNT).map { offset ->
                val date = from.plusDays(offset.toLong())
                val day = byDay[date.toEpochDay()]
                HistoryDay(
                    date = date,
                    calories = day?.calories ?: 0.0,
                    protein = day?.proteinGrams ?: 0.0,
                    carbs = day?.carbsGrams ?: 0.0,
                    fat = day?.fatGrams ?: 0.0,
                    burned = burnByDay[date.toEpochDay()]?.calories ?: 0.0,
                )
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(container) }
        }
    }
}
