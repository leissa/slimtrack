package com.leissa.slimtrack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.leissa.slimtrack.util.gramsText

fun progressFraction(value: Double, goal: Int): Float =
    if (goal > 0) (value / goal).coerceIn(0.0, 1.0).toFloat() else 0f

@Composable
fun CalorieProgressBar(value: Double, goal: Int, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progressFraction(value, goal) },
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape),
        drawStopIndicator = {},
    )
}

@Composable
fun MacroStat(label: String, value: Double, goal: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                "${value.gramsText()} / $goal g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progressFraction(value, goal) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            drawStopIndicator = {},
        )
    }
}
