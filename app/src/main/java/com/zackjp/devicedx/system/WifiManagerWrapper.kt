package com.zackjp.devicedx.system

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val WIFI_LEVELS = listOf(
    -80, // very weak: unusable
    -70, // weak: dropped packets
    -65, // fair: minimum recommended signal
    -50, // good: reliable for most apps
    // else = excellent: max data rates
)

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
            val qualityLevel = calculateSignalLevelInternal(rssi)
            val qualityPercent = qualityLevel.toFloat() / WIFI_LEVELS.size

            return WifiInfo(
                ipAddress = convertIntToIp(connectionInfo.ipAddress),
                linkSpeedMbps = connectionInfo.linkSpeed,
                ssid = connectionInfo.ssid,
                rssi = rssi,
                wifiStrength = qualityLevel,
                wifiStrengthPercent = qualityPercent,
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

    private fun calculateSignalLevelInternal(rssi: Int): Int {
        val level = WIFI_LEVELS.indexOfFirst { rssi < it }
        return if (level < 0) WIFI_LEVELS.size else level
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
    val rssi: Int = -100,
    val wifiStrength: Int = 0,
    val wifiStrengthPercent: Float = 0f,
)