package com.zackjp.devicedx.data

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {

    private val wifiScanResults = callbackFlow {
        val scanReceiver = createScanReceiverForFlow(this)
        registerWifiScanReceiver(scanReceiver)
        requestWifiScan()
        awaitClose {
            appContext.unregisterReceiver(scanReceiver)
        }
    }
        .stateIn(
            CoroutineScope(Dispatchers.Default),
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    fun getWifiScanFlow(): Flow<List<ScanResult>> = wifiScanResults

    private fun ProducerScope<List<ScanResult>>.requestWifiScan() {
        val wifiManager = appContext.getSystemService<WifiManager>()
        val scanResult = wifiManager?.startScan()
        if (scanResult == false) trySend(emptyList())
    }

    private fun registerWifiScanReceiver(scanReceiver: BroadcastReceiver): BroadcastReceiver {
        val scanIntentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        ContextCompat.registerReceiver(
            appContext,
            scanReceiver,
            scanIntentFilter,
            ContextCompat.RECEIVER_EXPORTED,
        )

        return scanReceiver
    }

    private fun createScanReceiverForFlow(scope: ProducerScope<List<ScanResult>>): BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val wifiStatePerm =
                    ContextCompat.checkSelfPermission(appContext, ACCESS_WIFI_STATE)
                val fineLocationPerm =
                    ContextCompat.checkSelfPermission(appContext, ACCESS_FINE_LOCATION)

                if (wifiStatePerm == PERMISSION_GRANTED && fineLocationPerm == PERMISSION_GRANTED) {
                    val wifiManager = appContext.getSystemService<WifiManager>()
                    val scanResults = wifiManager?.scanResults ?: emptyList()
                    scope.trySend(scanResults)
                }
            }
        }

}
