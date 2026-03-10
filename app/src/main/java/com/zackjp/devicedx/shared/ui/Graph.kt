package com.zackjp.devicedx.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max


private const val CUBIC_SMOOTHING_FACTOR = 0.5f

private val CANVAS_HEIGHT_THRESHOLD_FOR_FONT_SIZE = 300.dp

private val DefaultChartOutlineColor = Color.LightGray

data class LineConfig(
    val data: List<GraphEntry>,
    val color: Color,
)
data class GraphEntry(val x: Long, val y: Long)


@Composable
fun Graph(
    lines: List<LineConfig>,
    xTickStartValue: Long,
    xTickEndValue: Long,
    yTickTopValue: Long,
    yTickBottomValue: Long,
    yTickCount: Int,
    getYTickLabel: (yValue: Long) -> String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val path = remember { Path() }
    val path2 = remember { Path() }

    Canvas(modifier) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        /*
         * Strategy: Flip the canvas horizontally to support RTL. Within this block, calculate
         * coordinates for LTR as normal. Only needs custom support when drawing text – just
         * render the text backwards first using a scale(-1, 1) to negate this outer transform,
         * which will again flip the text around.
         */
        withTransform({
            if (isRtl) {
                scale(-1f, 1f)
            }
        }) {
            path.rewind()

            val axisChartMarginPx = 8.dp.toPx()

            /*
             * Calculate draw areas
             */
            val yTickIncrement = (yTickTopValue - yTickBottomValue) / yTickCount
            val yAxisLayoutResults = measureYAxisLabels(
                yTickCount = yTickCount,
                yTickIncrement = yTickIncrement,
                textMeasurer = textMeasurer,
                getYTickLabel = getYTickLabel,
            )

            val yAxisLabelArea = calculateYAxisLabelArea(yAxisLayoutResults)

            val halfYLabelHeight = (yAxisLayoutResults.maxLabelHeight / 2).toFloat()
            val chartArea = calculateChartArea(yAxisLabelArea, axisChartMarginPx, halfYLabelHeight)


            /*
             * Draw the chart elements
             */
            val yTickSpacing = chartArea.height / yTickCount
            drawYAxisLabels(
                yAxisLabelArea = yAxisLabelArea,
                yAxisLayoutResults = yAxisLayoutResults,
                yTickSpacing = yTickSpacing,
            )

            lines.forEach { lineConfig ->
                if (lineConfig.data.isNotEmpty()) {
                    val canvasPoints = mapDataToCanvasPoints(
                        data = lineConfig.data,
                        chartArea = chartArea,
                        xTickStartValue = xTickStartValue,
                        xTickEndValue = xTickEndValue,
                        yTickBottomValue = yTickBottomValue,
                        yTickTopValue = yTickTopValue,
                    )

                    drawDataLine(
                        canvasPoints = canvasPoints,
                        reusablePath = path,
                        reusablePath2 = path2,
                        lineColor = lineConfig.color,
                        chartArea = chartArea,
                    )
                }
            }

            drawChartOutline(
                reusablePath = path,
                chartArea = chartArea,
                color = DefaultChartOutlineColor,
            )
        }
    }
}

private fun DrawScope.calculateChartArea(
    yAxisLabelArea: Rect,
    axisChartMarginPx: Float,
    halfYLabelHeight: Float
): Rect = Rect(
    left = yAxisLabelArea.right + axisChartMarginPx,
    top = halfYLabelHeight,
    right = size.width,
    bottom = size.height - halfYLabelHeight,
)

private fun calculateYAxisLabelArea(
    yAxisLayoutResults: AxisLayoutResults,
): Rect = Rect(
    left = 0f,
    top = 0f,
    right = yAxisLayoutResults.maxLabelWidth.toFloat(),
    bottom = 0f,
)

private fun DrawScope.drawChartOutline(
    reusablePath: Path,
    chartArea: Rect,
    color: Color,
) {
    val strokeWidth = 2.dp.toPx()
    val topStart = chartArea.topLeft
    val bottomStart = chartArea.bottomLeft
    val bottomEnd = chartArea.bottomRight

    reusablePath.rewind()
    reusablePath.moveTo(topStart.x, topStart.y)
    reusablePath.lineTo(bottomStart.x, bottomStart.y)
    reusablePath.lineTo(bottomEnd.x, bottomEnd.y)

    drawPath(
        path = reusablePath,
        color = color,
        style = Stroke(width = strokeWidth)
    )
}

private fun DrawScope.drawDataLine(
    canvasPoints: List<Offset>,
    reusablePath: Path,
    reusablePath2: Path,
    lineColor: Color,
    chartArea: Rect,
) {
    reusablePath.rewind()

    val firstPoint = canvasPoints[0]
    reusablePath.moveTo(firstPoint.x, firstPoint.y)

    for (i in 0 until canvasPoints.lastIndex) {
        val currentPoint = canvasPoints[i]
        val nextPoint = canvasPoints[i + 1]

        val distanceX = nextPoint.x - currentPoint.x
        val controlDistance = distanceX * CUBIC_SMOOTHING_FACTOR
        reusablePath.cubicTo(
            x1 = currentPoint.x + controlDistance,
            y1 = currentPoint.y,
            x2 = nextPoint.x - controlDistance,
            y2 = nextPoint.y,
            x3 = nextPoint.x,
            y3 = nextPoint.y
        )
    }

    // Draw gradient area under line
    reusablePath2.apply {
        rewind()
        addPath(reusablePath)
        lineTo(canvasPoints.last().x, chartArea.bottomRight.y)
        lineTo(chartArea.bottomLeft.x, chartArea.bottomLeft.y)
        close()

        drawPath(
            path = this,
            brush = Brush.verticalGradient(
                0f to lineColor.copy(alpha = 0.35f),
                1f to Color.Transparent,
            ),
        )
    }

    // Draw line on top
    drawPath(
        path = reusablePath,
        color = lineColor,
        style = Stroke(2.dp.toPx())
    )
}

private fun mapDataToCanvasPoints(
    data: List<GraphEntry>,
    chartArea: Rect,
    xTickStartValue: Long,
    xTickEndValue: Long,
    yTickBottomValue: Long,
    yTickTopValue: Long,
): List<Offset> = data.map { graphEntry ->
    // Normalize width & height within chart area bounds. Map Y coordinate starting from bottom
    val xRangePercent =
        (graphEntry.x - xTickStartValue).toFloat() / (xTickEndValue - xTickStartValue)
    val yRangePercent =
        (graphEntry.y - yTickBottomValue).toFloat() / (yTickTopValue - yTickBottomValue)
    val normalizedX = xRangePercent * chartArea.width
    val normalizedY = yRangePercent * chartArea.height

    Offset(chartArea.left + normalizedX, chartArea.bottom - normalizedY)
}

private fun DrawScope.measureYAxisLabels(
    yTickCount: Int,
    yTickIncrement: Long,
    textMeasurer: TextMeasurer,
    getYTickLabel: (Long) -> String
): AxisLayoutResults {
    // Adjust font size for resizable Canvas, eg, PictureInPicture mode
    val fontScale =
        (size.height / CANVAS_HEIGHT_THRESHOLD_FOR_FONT_SIZE.toPx())
            .coerceIn(0.3f, 1.0f)

    val style = with(TextStyle.Default) {
        copy(fontSize = 18.sp * fontScale)
    }

    var maxYLabelWidthPx = 0
    var maxYLabelHeightPx = 0

    val layoutResults = (0 until yTickCount).map {
        val yTickValue = (it * yTickIncrement)
        val layoutResult = textMeasurer.measure(
            text = getYTickLabel(yTickValue),
            style = style,
        )
        maxYLabelWidthPx = max(maxYLabelWidthPx, layoutResult.size.width)
        maxYLabelHeightPx = max(maxYLabelHeightPx, layoutResult.size.height)
        layoutResult
    }

    return AxisLayoutResults(
        textLayoutResults = layoutResults,
        maxLabelWidth = maxYLabelWidthPx,
        maxLabelHeight = maxYLabelHeightPx,
    )
}

private fun DrawScope.drawYAxisLabels(
    yAxisLabelArea: Rect,
    yAxisLayoutResults: AxisLayoutResults,
    yTickSpacing: Float,
) {
    val layoutResults = yAxisLayoutResults.textLayoutResults

    layoutResults.forEachIndexed { tickIndex, layoutResult ->
        val x = yAxisLabelArea.right - layoutResult.size.width
        val y = size.height - tickIndex * yTickSpacing - layoutResult.size.height // align to bottom

        val isRtl = layoutDirection == LayoutDirection.Rtl
        withTransform({
            // If RTL, render text flipped horizontally around its own center point
            // to negate the outer canvas flip
            if (isRtl) {
                val centerPoint = Offset(
                    x + (layoutResult.size.width / 2),
                    y - (layoutResult.size.height / 2),
                )
                scale(-1f, 1f, centerPoint)
            }
        }) {
            drawText(
                color = Color.White,
                textLayoutResult = layoutResult,
                topLeft = Offset(x, y),
            )
        }
    }
}

fun Number.getScaleCount(unitScale: Int): Int {
    var tmp = abs(toInt())
    if (tmp == 0) return 1

    var count = 0
    while (tmp != 0) {
        tmp /= unitScale
        count++
    }
    return count
}

private data class AxisLayoutResults(
    val textLayoutResults: List<TextLayoutResult>,
    val maxLabelWidth: Int,
    val maxLabelHeight: Int,
)