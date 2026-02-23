package com.zackjp.devicedx.system

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WifiManagerWrapper @Inject constructor(
    @ApplicationContext appContext: Context,
) {

    private val wifiManager: WifiManager? =
        appContext.applicationContext.getSystemService<WifiManager>()

    fun getWifiSignalStrength(): Int {
        val rssi = wifiManager?.connectionInfo?.rssi ?: 0
        return when {
            rssi >= -60 -> 3 // excellent: max data rates
            rssi >= -70 -> 2 // good: reliable for most apps
            rssi >= -80 -> 1 // weak: dropped packets
            rssi >= -90 -> 0 // extremely weak: unusable
            else -> 0 // else: less than -90; unusable
        }
    }

    fun requestScan() {
        wifiManager?.startScan()
    }

    fun getCachedScanResults(): Result<List<ScanResult>> {
        return try {
            val results = wifiManager?.scanResults ?: emptyList()
            Result.success(results)
        } catch(e: SecurityException) {
            Result.failure(e)
        }
    }

}
