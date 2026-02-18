package com.zackjp.devicedx.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.pow



@Composable
fun <T> Graph(
    data: List<T>,
    maxDataPoints: Int,
    getY: (x: Int) -> Float?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        if (data.isEmpty() || maxDataPoints <= 0) return@Canvas

        val maxYValue = (0..<maxDataPoints).maxOfOrNull { getY(it) ?: 0f } ?: 0f
        val maxYAxisPoint = 10.0.pow(maxYValue.getDigitsCount())
        val spacing = size.width / (maxDataPoints - 1)

        val path = Path()

        // Map data to xy canvas coordinates
        val plotPoints = (0 until maxDataPoints).map { counter ->
            val dataIndex = when (layoutDirection) {
                LayoutDirection.Ltr -> data.size - maxDataPoints + counter
                LayoutDirection.Rtl -> data.lastIndex - counter
            }

            val rawY = getY(dataIndex)
            val actualX = counter * spacing
            val actualY = rawY?.let {
                // normalize height and render starting from bottom
                (size.height - (it / maxYAxisPoint) * size.height).toFloat()
            } ?: 0f

            Offset(actualX, actualY)
        }

        val firstPoint = plotPoints[0]
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 0 until plotPoints.lastIndex) {
            val current = plotPoints[i]
            val next = plotPoints[i+1]

            val distanceX = next.x - current.x
            val controlDistance = distanceX * CUBIC_SMOOTHING_FACTOR
            path.cubicTo(
                x1 = current.x + controlDistance,
                y1 = current.y,
                x2 = next.x - controlDistance,
                y2 = next.y,
                x3 = next.x,
                y3 = next.y
            )
        }

        drawPath(
            path = path,
            color = Color.Magenta,
            style = Stroke(2.dp.toPx())
        )
    }
}

private fun Number.getDigitsCount(): Int {
    var tmp = abs(toInt())
    if (tmp == 0) return 1

    var count = 0
    while (tmp != 0) {
        tmp /= 10
        count++
    }
    return count
}


private const val CUBIC_SMOOTHING_FACTOR = 0.5f
