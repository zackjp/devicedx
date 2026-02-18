package com.zackjp.devicedx.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    Surface(modifier) {
        val dxOptions = remember(navActions) {
            listOf(
                R.string.dashboard_open_wifi_monitor to navActions.toWifiMonitor,
                R.string.dashboard_open_latency_monitor to navActions.toLatencyMonitor,
                R.string.dashboard_open_traffic_monitor to navActions.toTrafficMonitor,
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(dxOptions) { index, dxOptionPair ->
                val (textResId, onClick) = dxOptionPair
                DiagnosticOption(
                    modifier = Modifier
                        .clickable(onClick = onClick)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    textResId = textResId,
                )

                if (index < dxOptions.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DiagnosticOption(
    modifier: Modifier = Modifier,
    textResId: Int,
) {
    Text(
        modifier = modifier,
        text = stringResource(textResId),
    )
}
