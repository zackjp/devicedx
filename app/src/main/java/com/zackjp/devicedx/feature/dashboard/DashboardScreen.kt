package com.zackjp.devicedx.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zackjp.devicedx.R
import com.zackjp.devicedx.navigation.NavActions


@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    navActions: NavActions,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            DiagnosticButtonRow(
                modifier = Modifier.fillMaxWidth(),
                navActions = navActions,
            )
        }
    }
}

@Composable
private fun DiagnosticButtonRow(
    modifier: Modifier = Modifier,
    navActions: NavActions,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = navActions.toWifiMonitor) {
            Text(stringResource(R.string.dashboard_open_wifi_monitor))
        }

        Button(onClick = navActions.toLatencyMonitor) {
            Text(stringResource(R.string.latency_monitor_open))
        }

        Button(onClick = navActions.toTrafficMonitor) {
            Text(stringResource(R.string.open_traffic_monitor))
        }
    }
}
