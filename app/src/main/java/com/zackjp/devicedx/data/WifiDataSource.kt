package com.zackjp.devicedx.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.zackjp.devicedx.di.ApplicationScope
import com.zackjp.devicedx.permissions.AppPermission
import com.zackjp.devicedx.system.ReceiverManager
import com.zackjp.devicedx.system.WifiManagerWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDataSource @Inject constructor(
    private val appPermission: AppPermission,
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

    fun getWifiScanFlow(): Flow<List<ScanResult>> = wifiScanResults

    private fun ProducerScope<List<ScanResult>>.createScanResultsIntentHandler(): (Context?, Intent?) -> Unit =
        handler@{ _, _ ->
            if (!appPermission.hasWifiState() || !appPermission.hasFineLocation()) {
                return@handler
            }

            val result = wifiManagerWrapper.getCachedScanResults()
            trySend(result.getOrDefault(emptyList()))
        }

}
