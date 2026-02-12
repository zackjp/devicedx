package com.zackjp.devicedx.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.permissions.AppPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
    private val appPermission: AppPermission,
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

    private var monitorJob: Job? = null

    fun onStartScan() {
        _screenState.update { it.copy(activeView = DashboardView.Wifi) }

        viewModelScope.launch {
            if (appPermission.hasFineLocation()) {
                _screenState.update { it.copy(permissionStatus = PermissionStatus.Granted) }
                initiateScan()
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

        viewModelScope.launch {
            initiateLatencyMonitor()
        }
    }

    private fun initiateScan() {
        monitorJob?.cancel()
        monitorJob = wifiDataSource.getWifiScanFlow()
            .map { scanResults ->
                scanResults.mapNotNull { it.SSID.ifEmpty { null } }
            }
            .onEach { wifiNames ->
                _screenState.update { it.copy(wifiNames = wifiNames) }
            }
            .launchIn(viewModelScope)
    }

    private fun initiateLatencyMonitor() {
        monitorJob?.cancel()
        monitorJob = realTimeNetworkDataSource.getLatencyMillisFlow()
            .onEach { latencyMillis ->
                _screenState.update {
                    it.copy(
                        latencyHistory =
                            (it.latencyHistory + latencyMillis).takeLast(MAX_LATENCY_DATA_POINTS)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun stopActiveMonitor() {
        monitorJob?.cancel()
        _screenState.update { it.copy(activeView = DashboardView.Unselected) }
    }

    companion object {
        const val MAX_LATENCY_DATA_POINTS = 10
    }
}
