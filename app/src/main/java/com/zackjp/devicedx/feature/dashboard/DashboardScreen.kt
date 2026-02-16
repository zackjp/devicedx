package com.zackjp.devicedx.feature.dashboard

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.feature.dashboard.DashboardViewModel.Companion.MAX_LATENCY_DATA_POINTS
import com.zackjp.devicedx.feature.dashboard.DashboardViewModel.Companion.TRAFFIC_METRICS_WINDOW_SECS
import com.zackjp.devicedx.model.TrafficMetric
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.pow


@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onStartScan()
            } else {
                viewModel.onFineLocationPermissionDenied()
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            if (it is DashboardEvent.LaunchFineLocation) {
                launcher.launch(ACCESS_FINE_LOCATION)
            }
        }
    }

    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            DiagnosticButtonRow(
                currentDashboardView = state.activeView,
                modifier = Modifier.fillMaxWidth(),
                onStartLatencyMonitor = { viewModel.onMonitorLatency() },
                onStartWifiScan = { viewModel.onStartScan() },
                onStartTrafficMonitor = { viewModel.onMonitorTraffic() },
                onStopCurrentMonitor = { viewModel.stopActiveMonitor() }
            )
        }

        when (state.activeView) {
            DashboardView.Unselected -> unselectedDiagnostics()
            DashboardView.Wifi -> wifiScanResults(state.wifiNames)
            DashboardView.Latency -> latencyGraph(state.latencyHistory)
            DashboardView.Traffic -> trafficGraph(state.trafficMetrics)
        }
    }
}

@Composable
private fun DiagnosticButtonRow(
    currentDashboardView: DashboardView,
    modifier: Modifier = Modifier,
    onStartLatencyMonitor: () -> Unit = {},
    onStartWifiScan: () -> Unit = {},
    onStartTrafficMonitor: () -> Unit = {},
    onStopCurrentMonitor: () -> Unit = {},
) {
    FlowRow(
        modifier = modifier,
    ) {
        if (currentDashboardView == DashboardView.Wifi) {
            Button(onClick = onStopCurrentMonitor) {
                Text(stringResource(R.string.stop_wifi_monitor))
            }
        } else {
            Button(onClick = onStartWifiScan) {
                Text(stringResource(R.string.get_wifi_ssids))
            }
        }

        Spacer(Modifier.width(16.dp))

        if (currentDashboardView == DashboardView.Latency) {
            Button(onClick = onStopCurrentMonitor) {
                Text(stringResource(R.string.stop_latency_monitor))
            }
        } else {
            Button(onClick = onStartLatencyMonitor) {
                Text(stringResource(R.string.get_network_speeds))
            }
        }

        Spacer(Modifier.width(16.dp))

        if (currentDashboardView == DashboardView.Traffic) {
            Button(onClick = onStopCurrentMonitor) {
                Text(stringResource(R.string.stop_traffic_monitor))
            }
        } else {
            Button(onClick = onStartTrafficMonitor) {
                Text(stringResource(R.string.start_traffic_monitor))
            }
        }
    }
}

private fun LazyListScope.unselectedDiagnostics() {
    item {
        Text(stringResource(R.string.select_your_diagnostic))
    }
}

private fun LazyListScope.wifiScanResults(
    wifiNames: List<String>,
) {
    items(wifiNames) { wifiName ->
        Spacer(Modifier.height(16.dp))
        Text(wifiName)
    }
}

private fun LazyListScope.latencyGraph(
    latencyHistory: List<Long>,
    modifier: Modifier = Modifier,
) {
    item {
        Column(modifier) {
            Text(stringResource(R.string.latency_ms, latencyHistory.lastOrNull() ?: "-"))
            Graph(
                data = latencyHistory,
                maxDataPoints = MAX_LATENCY_DATA_POINTS,
                getY = { if (it !in 0..latencyHistory.lastIndex) 0f else latencyHistory[it].toFloat() },
                modifier = Modifier
                    .background(Color.Black)
                    .fillMaxWidth()
                    .aspectRatio(1.5f),
            )
        }
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

@Composable
private fun <T> Graph(
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
            color = Color.White,
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


private val KB_SIZE = 1024.toBigDecimal()
private val MB_SIZE = 1_048_576.toBigDecimal()
private val GB_SIZE = 1_073_741_824.toBigDecimal()
private val TB_SIZE = 1_099_511_627_776.toBigDecimal()

private const val CUBIC_SMOOTHING_FACTOR = 0.5f
