package com.zackjp.devicedx.feature.traffic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.zackjp.devicedx.feature.traffic.ui.TrafficTimeline
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.shared.ui.PrimaryButton
import com.zackjp.devicedx.shared.ui.rememberIsInPipMode
import com.zackjp.devicedx.ui.theme.CyberAmber
import com.zackjp.devicedx.ui.theme.Turquoise
import kotlinx.coroutines.launch


private const val ASPECT_RATIO_NUMERATOR = 16
private const val ASPECT_RATIO_DENOMINATOR = 9
private const val ASPECT_RATIO_FLOAT = ASPECT_RATIO_NUMERATOR.toFloat() / ASPECT_RATIO_DENOMINATOR

private val SHEET_PEEK_HEIGHT = 48.dp

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
        if (isInPipMode) Modifier else modifier // excludes any padding when in PiP mode
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

@OptIn(ExperimentalMaterial3Api::class) // BottomSheetScaffold
@Composable
private fun TrafficMonitorScreen(
    isInPipMode: Boolean,
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit = {},
    onStopMonitor: () -> Unit = {},
    state: TrafficScreenState,
) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    if (isInPipMode) {
        MainContentPipMode(
            modifier = Modifier
                .fillMaxSize(),
            graphData = state.graphData,
        )
    } else {
        val coroutineScope = rememberCoroutineScope()
        BackHandler(enabled = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) {
            coroutineScope.launch {
                scaffoldState.bottomSheetState.partialExpand()
            }
        }

        BottomSheetScaffold(
            modifier = modifier,
            scaffoldState = scaffoldState,
            sheetContent = {
                TrafficTimeline(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    metrics = state.trafficSession?.trafficMetrics ?: emptyList(),
                )
            },
            sheetPeekHeight = SHEET_PEEK_HEIGHT
        ) {
            MainContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SHEET_PEEK_HEIGHT),
                state = state,
                onStartMonitor = onStartMonitor,
                onStopMonitor = onStopMonitor,
            )
        }
    }
}

@Composable
fun MainContentPipMode(
    modifier: Modifier = Modifier,
    graphData: List<TrafficMetric>,
) {
    Box(
        modifier = modifier,
    ) {
        TrafficGraphCard(
            trafficMetrics = graphData,
            rxLineColor = RxLineColor,
            txLineColor = TxLineColor,
            modifier = Modifier
                .fillMaxSize(), // Fill size. We already asked the OS to use a specific aspect ratio
        )
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    state: TrafficScreenState,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        val graphData = state.graphData

        TrafficGraphCard(
            trafficMetrics = graphData,
            rxLineColor = RxLineColor,
            txLineColor = TxLineColor,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ASPECT_RATIO_FLOAT),
        )

        Spacer(Modifier.height(12.dp))

        val mostRecentStat = graphData.lastOrNull()
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
            sessionStartTime = state.sessionStartTime,
            trafficSession = state.trafficSession,
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
