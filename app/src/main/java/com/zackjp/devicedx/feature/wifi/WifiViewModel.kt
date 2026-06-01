package com.zackjp.devicedx.feature.wifi

import android.net.wifi.ScanResult
import androidx.lifecycle.ViewModel
import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.system.WifiInfo
import com.zackjp.devicedx.system.permissions.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject


private val validStartMonitorStatuses = setOf(
    PermissionStatus.Unknown,
    PermissionStatus.DeniedTemporarily
)


@HiltViewModel
class WifiViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val permissionChecker: PermissionChecker,
    private val wifiDataSource: WifiDataSource,
) : ContainerHost<WifiScreenState, WifiScreenEffect>, ViewModel() {

    override val container = container<WifiScreenState, WifiScreenEffect>(
        initialState = WifiScreenState(
            permissionStatus = PermissionStatus.Unknown,
            wifiNames = emptyList(),
            wifiInfo = WifiInfo(),
        ),
    ) {
        coroutineScope {
            launch {
                observeWifiInfoFlow()
            }
            launch {
                observeActivatableWifiScan()
            }
        }
    }

    private val isMonitorActive = MutableStateFlow(false)

    fun startMonitor() = intent {
        if (permissionChecker.hasFineLocation()) {
            isMonitorActive.value = true
            reduce {
                state.copy(
                    permissionStatus = PermissionStatus.Granted,
                )
            }
        } else {
            if (state.permissionStatus in validStartMonitorStatuses) {
                reduce {
                    state.copy(
                        permissionStatus = PermissionStatus.Pending,
                    )
                }
                postSideEffect(WifiScreenEffect.LaunchFineLocation)
            }
        }
    }

    fun stopMonitor() {
        isMonitorActive.value = false
    }

    fun onFineLocationPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) = intent {
        if (isGranted) {
            startMonitor()
        } else {
            reduce {
                state.copy(
                    permissionStatus = if (shouldShowRationale)
                        PermissionStatus.DeniedTemporarily
                    else
                        PermissionStatus.DeniedPermanently
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun observeWifiInfoFlow() = subIntent {
        repeatOnSubscription {
            wifiDataSource.getWifiInfo()
                .onEach(::handleWifiInfo)
                .flowOn(dispatcherProvider.default)
                .launchIn(this)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, OrbitExperimental::class)
    private suspend fun observeActivatableWifiScan() = subIntent {
        repeatOnSubscription {
            isMonitorActive.flatMapLatest { isActive ->
                if (isActive) wifiDataSource.getWifiScanFlow() else emptyFlow()
            }
                .onEach(::handleWifiScanResults)
                .flowOn(dispatcherProvider.default)
                .launchIn(this)
        }
    }

    private fun handleWifiInfo(wifiInfo: WifiInfo) = intent {
        reduce {
            state.copy(
                wifiInfo = wifiInfo,
            )
        }
    }

    private fun handleWifiScanResults(scanResults: List<ScanResult>) = intent {
        reduce {
            state.copy(
                wifiNames = scanResults.mapNotNull { it.SSID.ifEmpty { null } }
            )
        }
    }

}
