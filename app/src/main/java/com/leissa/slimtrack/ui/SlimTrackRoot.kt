package com.leissa.slimtrack.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leissa.slimtrack.SlimTrackApp
import com.leissa.slimtrack.ui.history.HistoryScreen
import com.leissa.slimtrack.ui.history.HistoryViewModel
import com.leissa.slimtrack.ui.settings.SettingsScreen
import com.leissa.slimtrack.ui.settings.SettingsViewModel
import com.leissa.slimtrack.ui.today.TodayScreen
import com.leissa.slimtrack.ui.today.TodayViewModel
import com.leissa.slimtrack.ui.weight.WeightScreen
import com.leissa.slimtrack.ui.weight.WeightViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    Today("Today", Icons.Filled.Home),
    History("History", Icons.Filled.DateRange),
    Weight("Weight", Icons.Filled.Favorite),
    Profile("Profile", Icons.Filled.Person),
}

@Composable
fun SlimTrackRoot() {
    val container = (LocalContext.current.applicationContext as SlimTrackApp).container
    var tabName by rememberSaveable { mutableStateOf(Tab.Today.name) }
    val tab = Tab.valueOf(tabName)

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tabName = item.name },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (tab) {
            Tab.Today -> {
                val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(container))
                TodayScreen(viewModel, Modifier.padding(innerPadding))
            }

            Tab.History -> {
                val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
                HistoryScreen(viewModel, Modifier.padding(innerPadding))
            }

            Tab.Weight -> {
                val viewModel: WeightViewModel = viewModel(factory = WeightViewModel.factory(container))
                WeightScreen(viewModel, Modifier.padding(innerPadding))
            }

            Tab.Profile -> {
                val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
                SettingsScreen(viewModel, Modifier.padding(innerPadding))
            }
        }
    }
}
