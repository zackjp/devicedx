package com.zackjp.devicedx.feature.traffic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.zackjp.devicedx.navigation.NavActions
import com.zackjp.devicedx.shared.ui.PrimaryButton
import com.zackjp.devicedx.shared.ui.ScreenScaffold
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficMonitorScreenRoot(
    modifier: Modifier = Modifier,
    navActions: NavActions,
    viewModel: TrafficViewModel = hiltViewModel()
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val isInPipMode = rememberIsInPipMode(
        isAllowedProvider = { state.trafficDisplayInfo != null },
        aspectRatioNumerator = ASPECT_RATIO_NUMERATOR,
        aspectRatioDenominator = ASPECT_RATIO_DENOMINATOR,
    )

    ScreenScaffold(
        modifier = if (isInPipMode) Modifier else modifier, // excludes any padding when in PiP mode
        hideBars = isInPipMode,
        topBarActions = {
            IconButton(
                onClick = navActions.toTrafficHistory,
            ) {
                Icon(
                    contentDescription = null,
                    painter = painterResource(R.drawable.ic_rounded_history_24),
                )
            }
        }
    ) {
        TrafficMonitorScreen(
            consumeErrorAction = { viewModel.consumeErrorState() },
            isInPipMode = isInPipMode,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isInPipMode) Modifier else Modifier.padding(horizontal = 12.dp)
                ),
            onStartMonitor = viewModel::startMonitor,
            onStopMonitor = viewModel::stopMonitor,
            snackbarHostState = snackbarHostState,
            stateProvider = { state },
        )
    }
}

@Composable
private fun TrafficMonitorScreen(
    consumeErrorAction: () -> Unit,
    isInPipMode: Boolean,
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit = {},
    onStopMonitor: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    stateProvider: () -> TrafficScreenState,
) {
    if (isInPipMode) {
        MainContentPipMode(
            modifier = modifier,
            graphDataProvider = { stateProvider().graphData },
        )
    } else {
        TrafficErrorHandler(
            errorStatusProvider = { stateProvider().error },
            consumeErrorAction = consumeErrorAction,
            snackbarHostState = snackbarHostState,
        )

        Scaffold(
            contentWindowInsets = WindowInsets(), // zero padding since main app scaffold already applies it
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { localPadding ->
            MainContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(localPadding),
                stateProvider = stateProvider,
                onStartMonitor = onStartMonitor,
                onStopMonitor = onStopMonitor,
            )
        }
    }
}

@Composable
fun MainContentPipMode(
    modifier: Modifier = Modifier,
    graphDataProvider: () -> List<TrafficMetric>,
) {
    Box(
        modifier = modifier,
    ) {
        TrafficGraphCard(
            graphDataProvider = graphDataProvider,
            rxLineColor = RxLineColor,
            txLineColor = TxLineColor,
            modifier = Modifier
                .fillMaxSize(), // Fill size. We already asked the OS to use a specific aspect ratio
        )
    }
}

@Composable
private fun TrafficErrorHandler(
    snackbarHostState: SnackbarHostState,
    errorStatusProvider: () -> TrafficScreenError?,
    consumeErrorAction: () -> Unit,
) {
    val error = errorStatusProvider()
    val errorMessageString = stringResource(R.string.traffic_error_message_generic)

    LaunchedEffect(error) {
        if (error == TrafficScreenError.SessionError) {
            snackbarHostState.showSnackbar(
                message = errorMessageString,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            consumeErrorAction()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // BottomSheetScaffold
@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    stateProvider: () -> TrafficScreenState,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    BackHandler(enabled = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) {
        coroutineScope.launch {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    val isSessionActiveProvider = { stateProvider().trafficDisplayInfo != null }
    val trafficDisplayInfoProvider = { stateProvider().trafficDisplayInfo }

    Column(
        modifier = modifier,
    ) {
        TrafficGraphCard(
            graphDataProvider = { stateProvider().graphData },
            rxLineColor = RxLineColor,
            txLineColor = TxLineColor,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ASPECT_RATIO_FLOAT),
        )

        Spacer(Modifier.height(12.dp))

        BottomSheetScaffold(
            modifier = Modifier.weight(1f),
            scaffoldState = scaffoldState,
            sheetContent = {
                TrafficTimeline(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    metricsProvider = {
                        trafficDisplayInfoProvider()?.session?.trafficMetrics ?: emptyList()
                    },
                    isSessionActiveProvider = isSessionActiveProvider,
                    rxColor = RxLineColor,
                    txColor = TxLineColor,
                )
            },
            sheetPeekHeight = SHEET_PEEK_HEIGHT
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = SHEET_PEEK_HEIGHT),
            ) {
                ThroughputRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    statProvider = { stateProvider().graphData.lastOrNull() },
                    rxLineColor = RxLineColor,
                    txLineColor = TxLineColor,
                )

                Spacer(Modifier.height(12.dp))

                SessionInfoCard(
                    modifier = Modifier.fillMaxWidth(),
                    trafficDisplayInfoProvider = trafficDisplayInfoProvider,
                )

                Spacer(Modifier.weight(1f))

                MonitorButton(
                    modifier = Modifier.fillMaxWidth(),
                    isMonitorActiveProvider = isSessionActiveProvider,
                    onStartMonitor = onStartMonitor,
                    onStopMonitor = onStopMonitor,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MonitorButton(
    modifier: Modifier = Modifier,
    isMonitorActiveProvider: () -> Boolean,
    onStartMonitor: () -> Unit = {},
    onStopMonitor: () -> Unit = {},
) {
    val (textResId, onClick) = if (isMonitorActiveProvider()) {
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
