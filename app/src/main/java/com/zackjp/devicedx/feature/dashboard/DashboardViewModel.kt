package com.zackjp.devicedx.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val wifiDataSource: WifiDataSource,
    private val appPermission: AppPermission,
) : ViewModel() {

    private val _events = Channel<DashboardEvent>()
    val events = _events.receiveAsFlow()

    private val _screenState = MutableStateFlow(
        DashboardScreenState(
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

    private var scanJob: Job? = null

    fun onStartScan() {
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

    private fun initiateScan() {
        scanJob?.cancel()
        scanJob = wifiDataSource.getWifiScanFlow()
            .map { scanResults ->
                scanResults.mapNotNull { it.SSID.ifEmpty { null } }
            }
            .onEach { wifiNames ->
                _screenState.update { it.copy(wifiNames = wifiNames) }
            }
            .launchIn(viewModelScope)
    }

}
