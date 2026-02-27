package com.zackjp.devicedx.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs


private val CANVAS_HEIGHT_THRESHOLD_FOR_FONT_SIZE = 300.dp

data class GraphEntry(val x: Float, val y: Float)

@Composable
fun Graph(
    data: List<GraphEntry>,
    maxDataPoints: Int,
    unitScaleY: Int,
    getY: (index: Int) -> Float?,
    getYTickLabel: (yValue: Float) -> String,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Magenta,
) {
    val textMeasurer = rememberTextMeasurer()
    val path = remember { Path() }

    Canvas(modifier) {
        path.rewind()
        if (data.isEmpty() || maxDataPoints <= 0) return@Canvas

        val maxYValue = data.maxOfOrNull { it.y } ?: 0f
        val yAxisScale = maxYValue.getScaleCount(unitScaleY)
        val maxYTick = unitScaleY.toBigDecimal().pow(yAxisScale).toInt()
        val yTickCount = 4
        val yTickIncrement = maxYTick / yTickCount
        val yTickSpacing = size.height / yTickCount
        val xTickSpacing = size.width / (maxDataPoints - 1)

        // Map data to xy canvas coordinates
        val canvasPoints = (0 until maxDataPoints).map { xIndex ->
            val dataIndex = when (layoutDirection) {
                LayoutDirection.Ltr -> data.size - maxDataPoints + xIndex
                LayoutDirection.Rtl -> data.lastIndex - xIndex
            }

            val rawY = getY(dataIndex)
            val canvasX = xIndex * xTickSpacing
            val canvasY = rawY?.let {
                // normalize height within bounds and render starting from bottom
                val normalizedHeight = (it / maxYTick) * size.height
                size.height - normalizedHeight
            } ?: 0f

            Offset(canvasX, canvasY)
        }

        val firstPoint = canvasPoints[0]
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 0 until canvasPoints.lastIndex) {
            val currentPoint = canvasPoints[i]
            val nextPoint = canvasPoints[i + 1]

            val distanceX = nextPoint.x - currentPoint.x
            val controlDistance = distanceX * CUBIC_SMOOTHING_FACTOR
            path.cubicTo(
                x1 = currentPoint.x + controlDistance,
                y1 = currentPoint.y,
                x2 = nextPoint.x - controlDistance,
                y2 = nextPoint.y,
                x3 = nextPoint.x,
                y3 = nextPoint.y
            )
        }

        /*
         * Draw line graph
         */
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(2.dp.toPx())
        )

        /*
         * Draw y-axis labels
         */
        // Adjust font size for resizable Canvas, eg, PictureInPicture mode
        val fontScale = (size.height / CANVAS_HEIGHT_THRESHOLD_FOR_FONT_SIZE.toPx()).coerceIn(0.3f, 1.0f)
        val style = with (TextStyle.Default) { copy(fontSize = 18.sp * fontScale) }
        (0 until yTickCount).forEach {
            val yTickValue = (it * yTickIncrement).toFloat()
            val layoutResult = textMeasurer.measure(
                text = getYTickLabel(yTickValue),
                style = style,
            )
            val textOffset = Offset(
                0f,
                size.height - it * yTickSpacing - layoutResult.size.height,
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
