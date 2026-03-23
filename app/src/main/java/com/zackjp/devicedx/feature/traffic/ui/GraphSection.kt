package com.zackjp.devicedx.feature.traffic.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zackjp.devicedx.model.Bytes.Companion.asDataUnit
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.shared.ui.AppCard
import com.zackjp.devicedx.shared.ui.Graph
import com.zackjp.devicedx.shared.ui.GraphEntry
import com.zackjp.devicedx.shared.ui.LineConfig
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.max
import kotlin.math.min



private val yTickMaxSteps = listOf(
    5.toBigDecimal(),
    10.toBigDecimal(),
    15.toBigDecimal(),
    25.toBigDecimal(),
    50.toBigDecimal(),
    100.toBigDecimal(),
    150.toBigDecimal(),
    200.toBigDecimal(),
    250.toBigDecimal(),
    500.toBigDecimal(),
    750.toBigDecimal(),
    1000.toBigDecimal(),
    1030.toBigDecimal(), // covers the last 27 up until 1028
)

@Composable
internal fun TrafficGraphCard(
    graphDataProvider: () -> List<TrafficMetric>,
    rxLineColor: Color,
    txLineColor: Color,
    modifier: Modifier = Modifier,
) {
    val trafficMetrics = graphDataProvider()

    var xMinValue = if (trafficMetrics.isEmpty()) 0L else trafficMetrics[0].timestamp
    var xMaxValue = 0L
    var yMaxValue = 0L
    trafficMetrics.forEach {
        xMinValue = min(xMinValue, it.timestamp)
        xMaxValue = max(xMaxValue, it.timestamp)
        yMaxValue = max(yMaxValue, max(it.rxBytesPerSec, it.txBytesPerSec))
    }

    val yTickBytesMax = yMaxValue.asDataUnit(DataUnit.BYTE)
    val (yTickMaxDisplayableValue, yTickMaxDisplayableUnit) = yTickBytesMax.bestDisplayableUnit
    val yTickSteppedMax = yTickMaxSteps.firstOrNull { steppedValue ->
        yTickMaxDisplayableValue <= steppedValue
    } ?: yTickMaxDisplayableValue
    val yTickMaxValue = yTickSteppedMax.toLong().asDataUnit(yTickMaxDisplayableUnit)

    AppCard(
        modifier = modifier,
    ) {
        Graph(
            lines = listOf(
                LineConfig(
                    data = trafficMetrics.map { metric ->
                        GraphEntry(metric.timestamp, metric.txBytesPerSec)
                    },
                    color = txLineColor
                ),
                LineConfig(
                    data = trafficMetrics.map { metric ->
                        GraphEntry(metric.timestamp, metric.rxBytesPerSec)
                    },
                    color = rxLineColor
                ),
            ),
            xTickStartValue = xMinValue,
            xTickEndValue = xMaxValue,
            yTickBottomValue = 0L,
            yTickTopValue = yTickMaxValue.bytes,
            yTickCount = 5,
            getYTickLabel = { bytes ->
                val bytesValue = bytes.asDataUnit(DataUnit.BYTE)
                bytesValue.bestDisplayableUnit.run {
                    val number = first
                    val unitString = second.displayString
                    "${formatBigDecimal(number)}$unitString"
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        )
    }
}

private fun formatBigDecimal(number: BigDecimal): String {
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(java.util.Locale.US)).apply {
        isGroupingUsed = false
    }
    return decimalFormat.format(number)
}
