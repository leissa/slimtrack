package com.leissa.slimtrack.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leissa.slimtrack.data.AppContainer
import com.leissa.slimtrack.data.Energy
import com.leissa.slimtrack.data.Profile
import com.leissa.slimtrack.data.WeightEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WeightUiState(
    val entries: List<WeightEntry> = emptyList(),
    val profile: Profile = Profile(),
) {
    val unit get() = profile.unitSystem
    val latest: WeightEntry? get() = entries.lastOrNull()
    val bmi: Double get() = latest?.let { Energy.bmi(it.weightKg, profile.heightCm) } ?: 0.0
    val targetBmi: Double get() = Energy.bmi(profile.targetWeightKg, profile.heightCm)

    /** Share of the way from the first logged weight to the target, if meaningful. */
    val progress: Float?
        get() {
            val start = entries.firstOrNull()?.weightKg ?: return null
            val current = latest?.weightKg ?: return null
            if (start <= profile.targetWeightKg) return null
            val total = start - profile.targetWeightKg
            val done = start - current
            return (done / total).coerceIn(0.0, 1.0).toFloat()
        }
}

class WeightViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<WeightUiState> = combine(
        container.weightDao.observeAll(),
        container.profileDao.observe(),
    ) { entries, profile ->
        WeightUiState(entries = entries, profile = profile ?: Profile())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeightUiState(),
    )

    fun logWeight(date: LocalDate, weightKg: Double) {
        viewModelScope.launch {
            container.weightDao.upsert(
                WeightEntry(epochDay = date.toEpochDay(), weightKg = weightKg)
            )
        }
    }

    fun delete(entry: WeightEntry) {
        viewModelScope.launch { container.weightDao.delete(entry) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { WeightViewModel(container) }
        }
    }
}
