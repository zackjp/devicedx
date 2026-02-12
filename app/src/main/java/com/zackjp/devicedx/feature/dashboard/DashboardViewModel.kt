package com.zackjp.devicedx.feature.dashboard

import android.net.wifi.ScanResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.system.permissions.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val wifiDataSource: WifiDataSource,
    private val realTimeNetworkDataSource: RealTimeNetworkDataSource,
) : ViewModel() {

    private val _events = Channel<DashboardEvent>()
    val events = _events.receiveAsFlow()

    private val _screenState = MutableStateFlow(
        DashboardScreenState(
            activeView = DashboardView.Unselected,
            latencyHistory = emptyList(),
            permissionStatus = PermissionStatus.Unknown,
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
    private val latencyMonitorActive = MutableStateFlow(false)
    private val wifiScanActive = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activatableLatencyMonitor: Job = combine(
        uiActiveFlow,
        latencyMonitorActive,
    ) { uiActive, latencyMonitorActive ->
        uiActive && latencyMonitorActive
    }.flatMapLatest { activateLatencyMonitor ->
        if (activateLatencyMonitor) realTimeNetworkDataSource.getLatencyMillisFlow() else emptyFlow()
    }.onEach { latencyMillis ->
        handleNewLatencyMetric(latencyMillis)
    }.launchIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activatableWifiScanMonitor: Job = combine(
        uiActiveFlow,
        wifiScanActive,
    ) { uiActive, wifiScanActive ->
        uiActive && wifiScanActive
    }.flatMapLatest { activateWifiScan ->
        if (activateWifiScan) wifiDataSource.getWifiScanFlow() else emptyFlow()
    }.onEach { scanResults ->
        handleWifiScanResults(scanResults)
    }.launchIn(viewModelScope)

    fun onStartScan() {
        _screenState.update { it.copy(activeView = DashboardView.Wifi) }

        viewModelScope.launch {
            if (permissionChecker.hasFineLocation()) {
                _screenState.update { it.copy(permissionStatus = PermissionStatus.Granted) }
                initiateWifiScan()
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
        initiateLatencyMonitor()
    }

    private fun initiateWifiScan() {
        wifiScanActive.value = true
        latencyMonitorActive.value = false
    }

    private fun initiateLatencyMonitor() {
        latencyMonitorActive.value = true
        wifiScanActive.value = false
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

    fun stopActiveMonitor() {
        wifiScanActive.value = false
        latencyMonitorActive.value = false
        _screenState.update { it.copy(activeView = DashboardView.Unselected) }
    }

    companion object {
        const val MAX_LATENCY_DATA_POINTS = 10
    }
}
