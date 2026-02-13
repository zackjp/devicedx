package com.zackjp.devicedx.feature.dashboard

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.feature.dashboard.DashboardViewModel.Companion.MAX_LATENCY_DATA_POINTS


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
                onStopCurrentMonitor = { viewModel.stopActiveMonitor() }
            )
        }

        when (state.activeView) {
            DashboardView.Unselected -> unselectedDiagnostics()
            DashboardView.Wifi -> wifiScanResults(state.wifiNames)
            DashboardView.Latency -> latencyGraph(state.latencyHistory)
        }
    }
}

@Composable
private fun DiagnosticButtonRow(
    currentDashboardView: DashboardView,
    modifier: Modifier = Modifier,
    onStartLatencyMonitor: () -> Unit = {},
    onStartWifiScan: () -> Unit = {},
    onStopCurrentMonitor: () -> Unit = {},
) {
    Row(modifier) {
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
            Canvas(
                modifier = Modifier
                    .background(Color.Black)
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
            ) {
                if (latencyHistory.isEmpty()) return@Canvas

                val maxYAxisPoint = latencyHistory.max() / 1000 * 1000 + 1000
                val maxDataPoints = MAX_LATENCY_DATA_POINTS
                val spacing = size.width / maxDataPoints
                val halfSpacing = spacing / 2
                repeat(maxDataPoints) { counter ->
                    val index = if (layoutDirection == LayoutDirection.Ltr) {
                        latencyHistory.size - maxDataPoints + counter
                    } else {
                        latencyHistory.lastIndex - counter
                    }
                    if (index < 0) return@repeat

                    val latency = latencyHistory[index]
                    // normalize height and render starting from bottom
                    val y = size.height - (latency.toFloat() / maxYAxisPoint) * size.height
                    val x = counter * spacing + halfSpacing

                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
