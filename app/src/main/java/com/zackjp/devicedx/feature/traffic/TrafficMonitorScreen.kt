package com.zackjp.devicedx.feature.traffic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.model.Bytes.Companion.asDataUnit
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.shared.ui.Graph
import com.zackjp.devicedx.shared.ui.GraphEntry
import com.zackjp.devicedx.shared.ui.LineConfig
import com.zackjp.devicedx.shared.ui.rememberIsInPipMode
import com.zackjp.devicedx.ui.theme.OffWhite
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val ASPECT_RATIO_NUMERATOR = 16
private const val ASPECT_RATIO_DENOMINATOR = 9
private const val ASPECT_RATIO_FLOAT = ASPECT_RATIO_NUMERATOR.toFloat() / ASPECT_RATIO_DENOMINATOR

private val RxLineColor = Color(0xFF6F9FC6)
private val TxLineColor = Color(0xFF8FC2A6)
private val RxStatBgColor = Color(0xFF2B486A)
private val TxStatBgColor = Color(0xFF2C4F36)

@Composable
fun TrafficMonitorScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: TrafficViewModel = hiltViewModel()
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()

    val isInPipMode = rememberIsInPipMode(
        isAllowed = state.isMonitorActive,
        aspectRatioNumerator = ASPECT_RATIO_NUMERATOR,
        aspectRatioDenominator = ASPECT_RATIO_DENOMINATOR,
    )

    Surface(
        if (isInPipMode) Modifier else modifier
    ) {
        TrafficMonitorScreen(
            isInPipMode = isInPipMode,
            modifier = Modifier.fillMaxWidth(),
            onStartMonitor = viewModel::startMonitor,
            onStopMonitor = viewModel::stopMonitor,
            state = state,
        )
    }
}

@Composable
private fun TrafficMonitorScreen(
    isInPipMode: Boolean,
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit = {},
    onStopMonitor: () -> Unit = {},
    state: TrafficScreenState,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        TrafficGraphCard(
            trafficMetrics = state.trafficMetrics,
            rxLineColor = RxLineColor,
            txLineColor = TxLineColor,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Black)
                .fillMaxWidth()
                .aspectRatio(ASPECT_RATIO_FLOAT)
                .padding(12.dp),
        )

        if (!isInPipMode) {
            Spacer(Modifier.height(12.dp))

            val trafficMetrics = state.trafficMetrics
            val mostRecentStat = trafficMetrics.lastOrNull()
            TrafficStatsRow(
                modifier = Modifier.fillMaxWidth(),
                mostRecentStat = mostRecentStat,
            )

            val (textResId, onClick) = if (state.isMonitorActive) {
                R.string.stop_traffic_monitor to onStopMonitor
            } else {
                R.string.start_traffic_monitor to onStartMonitor
            }
            Button(onClick = onClick) {
                Text(stringResource(textResId))
            }
        }
    }

}

@Composable
private fun TrafficStatsRow(
    modifier: Modifier = Modifier,
    mostRecentStat: TrafficMetric?,
) {
    Row(modifier = modifier) {
        TrafficStat(
            modifier = Modifier.weight(1f),
            cardColor = RxStatBgColor,
            bytes = mostRecentStat?.rxBytesPerSec,
            label = stringResource(R.string.traffic_stat_label_rx),
        )
        Spacer(Modifier.width(8.dp))
        TrafficStat(
            modifier = Modifier.weight(1f),
            cardColor = TxStatBgColor,
            bytes = mostRecentStat?.txBytesPerSec,
            label = stringResource(R.string.traffic_stat_label_tx),
        )
    }
}

@Composable
fun TrafficStat(
    modifier: Modifier = Modifier,
    cardColor: Color,
    bytes: Long?,
    label: String,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                color = OffWhite,
                text = label,
                style = MaterialTheme.typography.bodySmall,
            )

            val valueText: String
            val unitText: String
            if (bytes == null) {
                valueText = "-"
                unitText = "MB/s"
            } else {
                val displayStat = bytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit
                valueText = displayStat.first.toPlainString()
                unitText = displayStat.second.displayString + "/s"
            }

            val formattedStat = buildAnnotatedString {
                withStyle(style = MaterialTheme.typography.headlineMedium.toSpanStyle()) {
                    append(valueText)
                }
                append(" ")
                withStyle(style = MaterialTheme.typography.headlineSmall.toSpanStyle()) {
                    append(unitText)
                }
            }

            Text(
                modifier = Modifier,
                color = OffWhite,
                text = formattedStat,
            )
        }
    }
}

@Composable
private fun TrafficGraphCard(
    trafficMetrics: List<TrafficMetric>,
    rxLineColor: Color,
    txLineColor: Color,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier,
    )
}

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

private fun formatBigDecimal(number: BigDecimal): String {
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US)).apply {
        isGroupingUsed = false
    }
    return decimalFormat.format(number)
}
