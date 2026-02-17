package com.zackjp.devicedx.feature.traffic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import java.math.MathContext
import java.math.RoundingMode

@Composable
fun TrafficMonitorScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: TrafficViewModel = hiltViewModel()
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()

    Surface(modifier) {
        TrafficMonitorScreen(
            modifier = Modifier.fillMaxSize(),
            onStartMonitor = { viewModel.startMonitor() },
            onStopMonitor = { viewModel.stopMonitor() },
            state = state,
        )
    }
}

@Composable
private fun TrafficMonitorScreen(
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit = {},
    onStopMonitor: () -> Unit = {},
    state: TrafficScreenState,
) {
    LazyColumn(modifier) {
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

        trafficGraph(state.trafficMetrics)
    }
}


private fun LazyListScope.trafficGraph(trafficMetrics: List<TrafficMetric>) {
    item {
        val mostRecentStat = trafficMetrics.lastOrNull()
        val txBytes = mostRecentStat?.rxBytesPerSec ?: 0f
        val txBigDecimal = txBytes.toBigDecimal(MathContext(2, RoundingMode.HALF_UP))
        val txUnit = when {
            txBigDecimal >= TB_SIZE -> "tb"
            txBigDecimal >= GB_SIZE -> "gb"
            txBigDecimal >= MB_SIZE -> "mb"
            txBigDecimal >= KB_SIZE -> "kb"
            else -> "b"
        }
        val txValue = when (txUnit) {
            "b" -> txBigDecimal
            "kb" -> txBigDecimal.divide(KB_SIZE, 2, RoundingMode.HALF_UP)
            "mb" -> txBigDecimal.divide(MB_SIZE, 2, RoundingMode.HALF_UP)
            "gb" -> txBigDecimal.divide(GB_SIZE, 2, RoundingMode.HALF_UP)
            else -> txBigDecimal.divide(TB_SIZE, 2, RoundingMode.HALF_UP)
        }
        Text("Recent Traffic Received: ${txValue.toPlainString()}$txUnit/sec")
        Graph(
            data = trafficMetrics,
            maxDataPoints = TRAFFIC_METRICS_WINDOW_SECS,
            getY = { if (it > trafficMetrics.lastIndex) 0f else trafficMetrics[it].rxBytesPerSec },
            modifier = Modifier
                .background(Color.Black)
                .fillMaxWidth()
                .aspectRatio(1.5f),
        )
    }
}


private val KB_SIZE = 1024.toBigDecimal()
private val MB_SIZE = 1_048_576.toBigDecimal()
private val GB_SIZE = 1_073_741_824.toBigDecimal()
private val TB_SIZE = 1_099_511_627_776.toBigDecimal()
