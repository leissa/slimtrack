package com.leissa.slimtrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leissa.slimtrack.data.AppContainer
import com.leissa.slimtrack.data.Goals
import com.leissa.slimtrack.data.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: Profile = Profile(),
    val goals: Goals = Goals(),
    val latestWeightKg: Double? = null,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        container.profileDao.observe(),
        container.goalsDao.observe(),
        container.weightDao.observeLatest(),
    ) { profile, goals, weight ->
        SettingsUiState(
            profile = profile ?: Profile(),
            goals = goals ?: Goals(),
            latestWeightKg = weight?.weightKg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun save(profile: Profile, goals: Goals) {
        viewModelScope.launch {
            container.profileDao.upsert(profile)
            container.goalsDao.upsert(goals)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
