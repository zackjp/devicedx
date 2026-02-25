package com.zackjp.devicedx.system

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WifiManagerWrapper @Inject constructor(
    @ApplicationContext appContext: Context,
) {

    private val wifiManager: WifiManager? =
        appContext.applicationContext.getSystemService<WifiManager>()

    fun getWifiInfo(): WifiInfo {
        val connectionInfo = wifiManager?.connectionInfo

        if (connectionInfo == null) {
            return WifiInfo()
        } else {
            val rssi = connectionInfo.rssi
            return WifiInfo(
                ipAddress = convertIntToIp(connectionInfo.ipAddress),
                linkSpeedMbps = connectionInfo.linkSpeed,
                ssid = connectionInfo.ssid,
                wifiStrength = when {
                    rssi >= -60 -> 3 // excellent: max data rates
                    rssi >= -70 -> 2 // good: reliable for most apps
                    rssi >= -80 -> 1 // weak: dropped packets
                    rssi >= -90 -> 0 // extremely weak: unusable
                    else -> 0 // else: less than -90; unusable
                },
            )
        }
    }

    fun requestScan() {
        wifiManager?.startScan()
    }

    fun getCachedScanResults(): Result<List<ScanResult>> {
        return try {
            val results = wifiManager?.scanResults ?: emptyList()
            Result.success(results)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

}

private fun convertIntToIp(ipAddress: Int): String {
    return String.format(
        Locale.US,
        "%d.%d.%d.%d",
        (ipAddress and 0xff),
        (ipAddress shr 8 and 0xff),
        (ipAddress shr 16 and 0xff),
        (ipAddress shr 24 and 0xff)
    )
}

data class WifiInfo(
    val ipAddress: String = "",
    val linkSpeedMbps: Int = 0,
    val ssid: String = "",
    val wifiStrength: Int = 0,
)