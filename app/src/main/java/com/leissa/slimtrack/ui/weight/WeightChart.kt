package com.leissa.slimtrack.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leissa.slimtrack.data.UnitSystem
import com.leissa.slimtrack.data.WeightEntry
import com.leissa.slimtrack.util.displayWeight
import com.leissa.slimtrack.util.shortDayLabel
import java.time.LocalDate

data class WeightPoint(
    val epochDay: Long,
    val kg: Double,
    val avg3: Double,
    val avg7: Double,
)

/** Builds the daily series plus trailing 3- and 7-day averages over logged days. */
fun weightPoints(entries: List<WeightEntry>): List<WeightPoint> = entries.map { entry ->
    fun average(days: Int): Double =
        entries.filter { it.epochDay in (entry.epochDay - days + 1)..entry.epochDay }
            .map { it.weightKg }
            .average()

    WeightPoint(
        epochDay = entry.epochDay,
        kg = entry.weightKg,
        avg3 = average(3),
        avg7 = average(7),
    )
}

@Composable
fun WeightChart(
    points: List<WeightPoint>,
    unit: UnitSystem,
    showDaily: Boolean,
    showAvg3: Boolean,
    showAvg7: Boolean,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val dailyColor = MaterialTheme.colorScheme.primary
    val avg3Color = MaterialTheme.colorScheme.tertiary
    val avg7Color = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        if (points.isEmpty()) return@Canvas

        val left = 42.dp.toPx()
        val right = size.width - 6.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 20.dp.toPx()

        val values = buildList {
            if (showDaily) points.forEach { add(it.kg) }
            if (showAvg3) points.forEach { add(it.avg3) }
            if (showAvg7) points.forEach { add(it.avg7) }
        }
        if (values.isEmpty()) return@Canvas

        var minValue = values.minOrNull() ?: return@Canvas
        var maxValue = values.maxOrNull() ?: return@Canvas
        if (maxValue - minValue < 0.2) {
            minValue -= 0.5
            maxValue += 0.5
        }
        val padding = (maxValue - minValue) * 0.08
        minValue -= padding
        maxValue += padding

        val minDay = points.first().epochDay
        val maxDay = points.last().epochDay
        val daySpan = (maxDay - minDay).toFloat()

        fun xFor(day: Long): Float =
            if (daySpan == 0f) (left + right) / 2f
            else left + (day - minDay) / daySpan * (right - left)

        fun yFor(value: Double): Float =
            bottom - ((value - minValue) / (maxValue - minValue)).toFloat() * (bottom - top)

        for (i in 0..2) {
            val value = minValue + (maxValue - minValue) * i / 2.0
            val y = yFor(value)
            drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            val measured = textMeasurer.measure(displayWeight(value, unit), labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(left - measured.size.width - 6.dp.toPx(), y - measured.size.height / 2f),
            )
        }

        fun drawSeries(selector: (WeightPoint) -> Double, color: Color, strokeWidth: Float) {
            val coords = points.map { point ->
                xFor(point.epochDay) to yFor(selector(point))
            }
            if (coords.size < 2) return
            val path = Path().apply {
                moveTo(coords.first().first, coords.first().second)
                for (index in 1 until coords.size) {
                    lineTo(coords[index].first, coords[index].second)
                }
            }
            drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        }

        if (showAvg7) drawSeries({ it.avg7 }, avg7Color, 2.dp.toPx())
        if (showAvg3) drawSeries({ it.avg3 }, avg3Color, 2.dp.toPx())
        if (showDaily) {
            drawSeries({ it.kg }, dailyColor, 1.5.dp.toPx())
            points.forEach { point ->
                drawCircle(
                    color = dailyColor,
                    radius = 3.dp.toPx(),
                    center = Offset(xFor(point.epochDay), yFor(point.kg)),
                )
            }
        }

        val startMeasured = textMeasurer.measure(LocalDate.ofEpochDay(minDay).shortDayLabel(), labelStyle)
        drawText(
            textLayoutResult = startMeasured,
            topLeft = Offset(left, size.height - startMeasured.size.height),
        )
        if (maxDay != minDay) {
            val endMeasured = textMeasurer.measure(LocalDate.ofEpochDay(maxDay).shortDayLabel(), labelStyle)
            drawText(
                textLayoutResult = endMeasured,
                topLeft = Offset(right - endMeasured.size.width, size.height - endMeasured.size.height),
            )
        }
    }
}
