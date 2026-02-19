package com.zackjp.devicedx.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs



@Composable
fun Graph(
    data: List<Pair<Float, Float>>,
    getY: (index: Int) -> Float?,
    getYTickLabel: (yValue: Float) -> String,
    maxDataPoints: Int,
    modifier: Modifier = Modifier,
    unitScaleY: Int,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        if (data.isEmpty() || maxDataPoints <= 0) return@Canvas

        val maxYValue = data.maxOfOrNull { it.second } ?: 0f
        val yAxisScale = maxYValue.getScaleCount(unitScaleY)
        val maxYTick = unitScaleY.toBigDecimal().pow(yAxisScale).toInt()
        val yTickCount = 4
        val yTickIncrement = maxYTick / yTickCount
        val yTickSpacing = size.height / yTickCount
        val xTickSpacing = size.width / (maxDataPoints - 1)

        val path = Path()

        // Map data to xy canvas coordinates
        val plotPoints = (0 until maxDataPoints).map { counter ->
            val dataIndex = when (layoutDirection) {
                LayoutDirection.Ltr -> data.size - maxDataPoints + counter
                LayoutDirection.Rtl -> data.lastIndex - counter
            }

            val rawY = getY(dataIndex)
            val graphX = counter * xTickSpacing
            val graphY = rawY?.let {
                // normalize height within bounds and render starting from bottom
                val normalizedHeight = (it / maxYTick) * size.height
                size.height - normalizedHeight
            } ?: 0f

            Offset(graphX, graphY)
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

        /*
         * Draw line graph
         */
        drawPath(
            path = path,
            color = Color.Magenta,
            style = Stroke(2.dp.toPx())
        )

        /*
         * Draw y-axis labels
         */
        (1..yTickCount).forEach {
            val yTickValue = (it * yTickIncrement).toFloat()
            val layoutResult = textMeasurer.measure(getYTickLabel(yTickValue))
            val textOffset = Offset(
                0f,
                size.height - it * yTickSpacing,
            )
            drawText(
                color = Color.White,
                textLayoutResult = layoutResult,
                topLeft = textOffset,
            )
        }
    }
}

private fun Number.getScaleCount(unitScale: Int): Int {
    var tmp = abs(toInt())
    if (tmp == 0) return 1

    var count = 0
    while (tmp != 0) {
        tmp /= unitScale
        count++
    }
    return count
}


private const val CUBIC_SMOOTHING_FACTOR = 0.5f
