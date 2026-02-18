package com.zackjp.devicedx.feature.wifi

import android.net.wifi.ScanResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.data.WifiDataSource
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class WifiViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val permissionChecker: PermissionChecker,
    private val wifiDataSource: WifiDataSource,
) : ViewModel() {

    private val _events = Channel<WifiScreenEvent>()
    val events = _events.receiveAsFlow()

    private val _screenState = MutableStateFlow(
        WifiScreenState(
            isMonitorActive = false,
            permissionStatus = PermissionStatus.Unknown,
            wifiNames = emptyList(),
        )
    )
    val screenState = _screenState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _screenState.value)

    private val uiActiveFlow = _screenState.subscriptionCount.map { it > 0 }.distinctUntilChanged()
    private val isMonitorActive = MutableStateFlow(false)

    private val activatableWifiScanMonitor: Job = uiActivatedFlow(
        dataSourceProvider = wifiDataSource::getWifiScanFlow,
    )
        .onEach(::handleWifiScanResults)
        .flowOn(dispatcherProvider.default)
        .launchIn(viewModelScope)

    fun startMonitor() {
        viewModelScope.launch {
            if (permissionChecker.hasFineLocation()) {
                _screenState.update {
                    it.copy(
                        isMonitorActive = true,
                        permissionStatus = PermissionStatus.Granted,
                    )
                }
                isMonitorActive.value = true
            } else if (_screenState.value.permissionStatus == PermissionStatus.Unknown) {
                _screenState.update { it.copy(permissionStatus = PermissionStatus.Pending) }
                _events.send(WifiScreenEvent.LaunchFineLocation)
            }
        }
    }

    fun onFineLocationPermissionDenied() {
        _screenState.update { it.copy(permissionStatus = PermissionStatus.Denied) }
    }

    private fun handleWifiScanResults(scanResults: List<ScanResult>) {
        _screenState.update { currentState ->
            val wifiNames = scanResults.mapNotNull { it.SSID.ifEmpty { null } }
            currentState.copy(wifiNames = wifiNames)
        }
    }


    fun stopMonitor() {
        _screenState.update { it.copy(isMonitorActive = false) }
        isMonitorActive.value = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> uiActivatedFlow(
        dataSourceProvider: () -> Flow<T>,
    ): Flow<T> = combine(
        uiActiveFlow,
        isMonitorActive,
    ) { uiActive, isMonitorActive ->
        uiActive && isMonitorActive
    }.flatMapLatest { isMonitorActive ->
        if (isMonitorActive) dataSourceProvider() else emptyFlow()
    }

}
