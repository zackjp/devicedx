package com.zackjp.devicedx.feature.traffic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.feature.traffic.ui.SessionInfoCard
import com.zackjp.devicedx.feature.traffic.ui.ThroughputRow
import com.zackjp.devicedx.feature.traffic.ui.TrafficGraphCard
import com.zackjp.devicedx.shared.ui.PrimaryButton
import com.zackjp.devicedx.shared.ui.rememberIsInPipMode
import com.zackjp.devicedx.ui.theme.CyberAmber
import com.zackjp.devicedx.ui.theme.Turquoise


private const val ASPECT_RATIO_NUMERATOR = 16
private const val ASPECT_RATIO_DENOMINATOR = 9
private const val ASPECT_RATIO_FLOAT = ASPECT_RATIO_NUMERATOR.toFloat() / ASPECT_RATIO_DENOMINATOR

private val RxLineColor = Turquoise
private val TxLineColor = CyberAmber


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
        val trafficMetrics = state.trafficSession?.trafficMetrics ?: emptyList()

        TrafficGraphCard(
            trafficMetrics = trafficMetrics,
            rxLineColor = RxLineColor,
            txLineColor = TxLineColor,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ASPECT_RATIO_FLOAT),
        )

        if (!isInPipMode) {
            Spacer(Modifier.height(12.dp))

            val mostRecentStat = trafficMetrics.lastOrNull()
            ThroughputRow(
                modifier = Modifier.fillMaxWidth(),
                mostRecentStat = mostRecentStat,
                rxLineColor = RxLineColor,
                txLineColor = TxLineColor,
            )

            Spacer(Modifier.height(12.dp))

            SessionInfoCard(
                modifier = Modifier.fillMaxWidth(),
                isActive = state.isMonitorActive,
                sessionId = state.trafficSession?.id,
                sessionStartTime = state.sessionStartTime,
                totalRxBytes = state.trafficSession?.totalRxBytes,
                totalTxBytes = state.trafficSession?.totalTxBytes,
            )

            Spacer(Modifier.weight(1f))

            MonitorButton(
                modifier = Modifier.fillMaxWidth(),
                isMonitorActive = state.isMonitorActive,
                onStartMonitor = onStartMonitor,
                onStopMonitor = onStopMonitor,
            )

            Spacer(Modifier.height(16.dp))
        }
    }

}

@Composable
fun MonitorButton(
    modifier: Modifier = Modifier,
    isMonitorActive: Boolean,
    onStartMonitor: () -> Unit = {},
    onStopMonitor: () -> Unit = {},
) {
    val (textResId, onClick) = if (isMonitorActive) {
        R.string.stop_traffic_monitor to onStopMonitor
    } else {
        R.string.start_traffic_monitor to onStartMonitor
    }

    PrimaryButton(
        modifier = modifier,
        onClick = onClick,
        text = stringResource(textResId),
    )
}
