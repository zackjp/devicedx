package com.zackjp.devicedx.feature.dashboard

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.navigation.NavActions


@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    navActions: NavActions,
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
                navActions = navActions,
                onStartWifiScan = { viewModel.onStartScan() },
                onStopCurrentMonitor = { viewModel.stopActiveMonitor() }
            )
        }

        when (state.activeView) {
            DashboardView.Unselected -> unselectedDiagnostics()
            DashboardView.Wifi -> wifiScanResults(state.wifiNames)
        }
    }
}

@Composable
private fun DiagnosticButtonRow(
    currentDashboardView: DashboardView,
    modifier: Modifier = Modifier,
    navActions: NavActions,
    onStartWifiScan: () -> Unit = {},
    onStopCurrentMonitor: () -> Unit = {},
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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

        Button(onClick = navActions.toLatencyMonitor) {
            Text(stringResource(R.string.latency_monitor_open))
        }

        Button(onClick = navActions.toTrafficMonitor) {
            Text(stringResource(R.string.open_traffic_monitor))
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
