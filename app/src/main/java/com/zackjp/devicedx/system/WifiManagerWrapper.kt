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
