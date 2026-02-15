package com.zackjp.devicedx.feature.dashboard

import android.net.wifi.ScanResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.feature.dashboard.util.TrafficGraphUtil
import com.zackjp.devicedx.model.TrafficData
import com.zackjp.devicedx.system.permissions.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val clock: Clock,
    dispatcherProvider: DispatcherProvider,
    private val permissionChecker: PermissionChecker,
    private val realTimeNetworkDataSource: RealTimeNetworkDataSource,
    private val trafficGraphUtil: TrafficGraphUtil,
    private val wifiDataSource: WifiDataSource,
) : ViewModel() {

    private val _events = Channel<DashboardEvent>()
    val events = _events.receiveAsFlow()

    private val _screenState = MutableStateFlow(
        DashboardScreenState(
            activeView = DashboardView.Unselected,
            latencyHistory = emptyList(),
            permissionStatus = PermissionStatus.Unknown,
            trafficMetrics = emptyList(),
            wifiNames = emptyList(),
        )
    )
    val screenState = _screenState
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _screenState.value,
        )

    private val uiActiveFlow = _screenState.subscriptionCount.map { it > 0 }.distinctUntilChanged()
    private val currentActiveMonitor: MutableStateFlow<DashboardView?> = MutableStateFlow(null)

    private val activatableLatencyMonitor: Job = viewEnabledFlow(
        activeView = DashboardView.Latency,
        dataSourceProvider = { realTimeNetworkDataSource.getLatencyMillisFlow() },
    ).onEach { latencyMillis ->
        handleNewLatencyMetric(latencyMillis)
    }.flowOn(dispatcherProvider.default).launchIn(viewModelScope)

    private val activatableWifiScanMonitor: Job = viewEnabledFlow(
        activeView = DashboardView.Wifi,
        dataSourceProvider = { wifiDataSource.getWifiScanFlow() },
    ).onEach { scanResults ->
        handleWifiScanResults(scanResults)
    }.flowOn(dispatcherProvider.default).launchIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activatableTrafficMonitor: Job = viewEnabledFlow(
        activeView = DashboardView.Traffic,
        dataSourceProvider = { realTimeNetworkDataSource.getTrafficStats() },
    ).runningFold(emptyList<TrafficData>()) { acc, trafficData ->
        val startTimeCutoff = clock.now()
            .minus(TRAFFIC_METRICS_WINDOW_SECS.seconds)
            .toEpochMilliseconds()
        acc.filter { it.timestamp >= startTimeCutoff } + trafficData
    }.onEach { trafficStats ->
        handleTrafficStats(trafficStats)
    }.flowOn(dispatcherProvider.default).launchIn(viewModelScope)

    fun onStartScan() {
        _screenState.update { it.copy(activeView = DashboardView.Wifi) }

        viewModelScope.launch {
            if (permissionChecker.hasFineLocation()) {
                _screenState.update { it.copy(permissionStatus = PermissionStatus.Granted) }
                currentActiveMonitor.value = DashboardView.Wifi
            } else if (_screenState.value.permissionStatus == PermissionStatus.Unknown) {
                _screenState.update { it.copy(permissionStatus = PermissionStatus.Pending) }
                _events.send(DashboardEvent.LaunchFineLocation)
            }
        }
    }

    fun onFineLocationPermissionDenied() {
        _screenState.update { it.copy(permissionStatus = PermissionStatus.Denied) }
    }

    fun onMonitorLatency() {
        _screenState.update { it.copy(activeView = DashboardView.Latency) }
        currentActiveMonitor.value = DashboardView.Latency
    }

    fun onMonitorTraffic() {
        _screenState.update { it.copy(activeView = DashboardView.Traffic) }
        currentActiveMonitor.value = DashboardView.Traffic
    }

    private fun handleWifiScanResults(scanResults: List<ScanResult>) {
        _screenState.update { currentState ->
            val wifiNames = scanResults.mapNotNull { it.SSID.ifEmpty { null } }
            currentState.copy(wifiNames = wifiNames)
        }
    }

    private fun handleNewLatencyMetric(latencyMillis: Long) {
        _screenState.update {
            it.copy(
                latencyHistory =
                    (it.latencyHistory + latencyMillis).takeLast(MAX_LATENCY_DATA_POINTS)
            )
        }
    }

    private fun handleTrafficStats(trafficHistory: List<TrafficData>) {
        val now = clock.now()

        val trafficMetrics = trafficGraphUtil.calculateMetrics(
            data = trafficHistory,
            endTime = now.toEpochMilliseconds(),
            TRAFFIC_METRICS_WINDOW_SECS.seconds
        )

        _screenState.update { it.copy(trafficMetrics = trafficMetrics) }
    }

    fun stopActiveMonitor() {
        currentActiveMonitor.value = DashboardView.Unselected
        _screenState.update { it.copy(activeView = DashboardView.Unselected) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> viewEnabledFlow(
        activeView: DashboardView,
        dataSourceProvider: () -> Flow<T>,
    ): Flow<T> = combine(
        uiActiveFlow,
        currentActiveMonitor,
    ) { uiActive, currentActiveMonitor ->
        uiActive && currentActiveMonitor == activeView
    }.flatMapLatest { isMonitorActive ->
        if (isMonitorActive) dataSourceProvider() else emptyFlow()
    }

    companion object {
        const val MAX_LATENCY_DATA_POINTS = 10
        const val TRAFFIC_METRICS_WINDOW_SECS = 30
    }
}
