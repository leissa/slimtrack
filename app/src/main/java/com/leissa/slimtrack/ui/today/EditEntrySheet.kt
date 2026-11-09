@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.leissa.slimtrack.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leissa.slimtrack.data.EntryWithFood
import com.leissa.slimtrack.util.gramsText
import com.leissa.slimtrack.util.kcalText

/** Lets the user correct the amount of an already logged food. */
@Composable
fun EditEntrySheet(
    entry: EntryWithFood,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by rememberSaveable(entry.entry.id) {
        mutableDoubleStateOf(entry.entry.quantity)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
        ) {
            Text("Edit entry", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(entry.food.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${entry.food.calories} kcal per ${entry.food.servingLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            ServingInput(
                servingLabel = entry.food.servingLabel,
                quantity = quantity,
                onQuantityChange = { quantity = it },
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = "${(entry.food.calories * quantity).kcalText()} kcal · " +
                    "P ${(entry.food.proteinGrams * quantity).gramsText()} · " +
                    "C ${(entry.food.carbsGrams * quantity).gramsText()} · " +
                    "F ${(entry.food.fatGrams * quantity).gramsText()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(quantity) },
                    enabled = quantity > 0.0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
            }
        }
    }
}
