package com.zackjp.devicedx.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.zackjp.devicedx.di.ApplicationScope
import com.zackjp.devicedx.system.ReceiverManager
import com.zackjp.devicedx.system.WifiInfo
import com.zackjp.devicedx.system.WifiManagerWrapper
import com.zackjp.devicedx.system.permissions.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDataSource @Inject constructor(
    private val permissionChecker: PermissionChecker,
    @ApplicationScope appScope: CoroutineScope,
    private val receiverManager: ReceiverManager,
    private val wifiManagerWrapper: WifiManagerWrapper,
) {

    private val wifiScanResults = callbackFlow {
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        val scanReceiver = receiverManager.registerReceiver(
            intentFilter,
            createScanResultsIntentHandler(),
        )
        wifiManagerWrapper.requestScan()
        awaitClose {
            receiverManager.unregisterReceiver(scanReceiver)
        }
    }
        .stateIn(
            appScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    private val wifiInfo = flow {
        while (true) {
            emit(wifiManagerWrapper.getWifiInfo())
            delay(2000)
        }
    }
        .stateIn(
            appScope,
            SharingStarted.WhileSubscribed(1500),
            WifiInfo(),
        )

    fun getWifiScanFlow(): Flow<List<ScanResult>> = wifiScanResults

    fun getWifiInfo(): Flow<WifiInfo> = wifiInfo

    private fun ProducerScope<List<ScanResult>>.createScanResultsIntentHandler(): (Context?, Intent?) -> Unit =
        handler@{ _, _ ->
            if (!permissionChecker.hasWifiState() || !permissionChecker.hasFineLocation()) {
                return@handler
            }

            val result = wifiManagerWrapper.getCachedScanResults()
            trySend(result.getOrDefault(emptyList()))
        }

}
