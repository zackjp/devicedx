package com.zackjp.devicedx.system.permissions

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {

    fun hasFineLocation(): Boolean {
        val access = ContextCompat.checkSelfPermission(appContext, ACCESS_FINE_LOCATION)
        return access == PERMISSION_GRANTED
    }

    fun hasWifiState(): Boolean {
        val access = ContextCompat.checkSelfPermission(appContext, ACCESS_WIFI_STATE)
        return access == PERMISSION_GRANTED
    }

}