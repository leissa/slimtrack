package com.leissa.slimtrack.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.leissa.slimtrack.util.ServingMeasure
import com.leissa.slimtrack.util.parseServingMeasure
import com.leissa.slimtrack.util.quantityText
import com.leissa.slimtrack.util.toDecimalOrNull

private const val MIN_SERVINGS = 0.05
private const val MAX_SERVINGS = 20.0

/**
 * Edits how much of a food is logged.
 *
 * Foods measured in grams or millilitres ("100 g", "250 ml") are entered in
 * their own unit, so a teaspoon of oil is 5 ml instead of "0.05 servings".
 * Portion labels ("1 medium", "1 slice") keep the servings stepper.
 */
@Composable
fun ServingInput(
    servingLabel: String,
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measure = remember(servingLabel) { parseServingMeasure(servingLabel) }
    if (measure != null) {
        AmountInput(measure, quantity, onQuantityChange, modifier)
    } else {
        ServingsInput(quantity, onQuantityChange, modifier)
    }
}

@Composable
private fun AmountInput(
    measure: ServingMeasure,
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    modifier: Modifier,
) {
    // The text field is the source of truth while editing so partial input such
    // as "0." does not get rewritten. Recreated when the measured serving changes.
    var text by rememberSaveable(measure.amount, measure.unit) {
        mutableStateOf(measure.amountFor(quantity).quantityText())
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
            text = filtered
            val amount = filtered.toDecimalOrNull()
            if (amount != null && amount > 0.0 && amount <= measure.amount * MAX_SERVINGS) {
                onQuantityChange(measure.servingsFor(amount))
            }
        },
        label = { Text("Amount") },
        suffix = { Text(measure.unit) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ServingsInput(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    modifier: Modifier,
) {
    var text by rememberSaveable { mutableStateOf(quantity.quantityText()) }

    fun setQuantity(value: Double) {
        val clamped = value.coerceIn(MIN_SERVINGS, MAX_SERVINGS)
        text = clamped.quantityText()
        onQuantityChange(clamped)
    }

    // Fine steps below one serving make half portions easy to reach.
    val step = if (quantity < 1.0) 0.1 else 0.5

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = { setQuantity(quantity - step) },
            modifier = Modifier.semantics { contentDescription = "Fewer servings" },
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
                text = filtered
                val parsed = filtered.toDecimalOrNull()
                if (parsed != null && parsed > 0.0) {
                    onQuantityChange(parsed.coerceAtMost(MAX_SERVINGS))
                }
            },
            label = { Text("Servings") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(
            onClick = { setQuantity(quantity + step) },
            modifier = Modifier.semantics { contentDescription = "More servings" },
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
