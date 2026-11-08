@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.leissa.slimtrack.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.leissa.slimtrack.data.Energy
import com.leissa.slimtrack.data.ExerciseType
import com.leissa.slimtrack.util.kcalText

@Composable
fun AddExerciseSheet(
    weightKg: Double?,
    onDismiss: () -> Unit,
    onAdd: (ExerciseType, Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activityName by rememberSaveable { mutableStateOf(ExerciseType.WALKING.name) }
    var minutes by rememberSaveable { mutableIntStateOf(30) }
    val activity = ExerciseType.valueOf(activityName)
    val burn = Energy.exerciseCalories(activity.met, weightKg ?: 75.0, minutes)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Log exercise", style = MaterialTheme.typography.headlineSmall)
            if (weightKg == null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Log your weight for a precise estimate; 75 kg is used for now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))

            Text("Activity", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseType.entries.forEach { type ->
                    FilterChip(
                        selected = activity == type,
                        onClick = { activityName = type.name },
                        label = { Text(type.label) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(
                    onClick = { minutes = (minutes - 5).coerceAtLeast(5) },
                    modifier = Modifier.semantics { contentDescription = "Less time" },
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "$minutes min",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                FilledTonalIconButton(
                    onClick = { minutes = (minutes + 5).coerceAtMost(300) },
                    modifier = Modifier.semantics { contentDescription = "More time" },
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "≈ ${burn.kcalText()} kcal burned",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onAdd(activity, minutes) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add exercise")
            }
        }
    }
}
