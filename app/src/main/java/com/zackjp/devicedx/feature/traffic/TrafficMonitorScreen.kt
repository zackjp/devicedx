package com.zackjp.devicedx.feature.traffic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.feature.traffic.TrafficViewModel.Companion.TRAFFIC_METRICS_WINDOW_SECS
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.shared.ui.Graph
import com.zackjp.devicedx.shared.ui.GraphEntry
import com.zackjp.devicedx.shared.ui.rememberIsInPipMode
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val ASPECT_RATIO_NUMERATOR = 16
private const val ASPECT_RATIO_DENOMINATOR = 9
private const val ASPECT_RATIO_FLOAT = ASPECT_RATIO_NUMERATOR.toFloat() / ASPECT_RATIO_DENOMINATOR

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
            onStartMonitor = { viewModel.startMonitor() },
            onStopMonitor = { viewModel.stopMonitor() },
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
    LazyColumn(modifier) {
        if (!isInPipMode) {
            item {
                if (state.isMonitorActive) {
                    Button(onClick = onStopMonitor) {
                        Text(stringResource(R.string.stop_traffic_monitor))
                    }
                } else {
                    Button(onClick = onStartMonitor) {
                        Text(stringResource(R.string.start_traffic_monitor))
                    }
                }
            }

            item {
                val trafficMetrics = state.trafficMetrics
                val mostRecentStat = trafficMetrics.lastOrNull()
                val rxBytes = mostRecentStat?.rxBytesPerSec ?: 0f
                val (rxValue, rxUnit) = getBytesString(rxBytes)
                Text("Recent Traffic Received: ${formatBigDecimal(rxValue)}$rxUnit/sec")
            }
        }

        trafficGraph(
            modifier = Modifier
                .background(Color.Black)
                .fillMaxWidth()
                .aspectRatio(ASPECT_RATIO_FLOAT),
            state.trafficMetrics,
        )
    }
}

private fun LazyListScope.trafficGraph(
    modifier: Modifier = Modifier,
    trafficMetrics: List<TrafficMetric>,
) {
    item {
        Graph(
            data = trafficMetrics.mapIndexed { index, metric ->
                GraphEntry(index.toFloat(), metric.rxBytesPerSec)
            },
            getY = { if (it > trafficMetrics.lastIndex) 0f else trafficMetrics[it].rxBytesPerSec },
            getYTickLabel = { bytes ->
                getBytesString(bytes).run {
                    "${formatBigDecimal(first)}$second"
                }
            },
            maxDataPoints = TRAFFIC_METRICS_WINDOW_SECS,
            modifier = modifier,
            unitScaleY = 128,
        )
    }
}


private fun getBytesString(bytes: Float): Pair<BigDecimal, String> {
    val bigDecimalValue = bytes.toBigDecimal()
    val unitString = when {
        bigDecimalValue >= TB_SIZE -> "tb"
        bigDecimalValue >= GB_SIZE -> "gb"
        bigDecimalValue >= MB_SIZE -> "mb"
        bigDecimalValue >= KB_SIZE -> "kb"
        else -> "b"
    }
    val unitValue = when (unitString) {
        "b" -> bigDecimalValue
        "kb" -> bigDecimalValue.divide(KB_SIZE, 2, RoundingMode.HALF_UP)
        "mb" -> bigDecimalValue.divide(MB_SIZE, 2, RoundingMode.HALF_UP)
        "gb" -> bigDecimalValue.divide(GB_SIZE, 2, RoundingMode.HALF_UP)
        else -> bigDecimalValue.divide(TB_SIZE, 2, RoundingMode.HALF_UP)
    }
    return Pair(unitValue, unitString)
}


fun formatBigDecimal(number: BigDecimal): String {
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US)).apply {
        isGroupingUsed = false
    }
    return decimalFormat.format(number)
}

private val KB_SIZE = 1024.toBigDecimal()
private val MB_SIZE = 1_048_576.toBigDecimal()
private val GB_SIZE = 1_073_741_824.toBigDecimal()
private val TB_SIZE = 1_099_511_627_776.toBigDecimal()
