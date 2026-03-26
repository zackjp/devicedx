package com.zackjp.devicedx.feature.wifi

import androidx.compose.runtime.Immutable
import com.zackjp.devicedx.system.WifiInfo

@Immutable
data class WifiScreenState(
    val isMonitorActive: Boolean,
    val permissionStatus: PermissionStatus,
    val wifiNames: List<String>,
    val wifiInfo: WifiInfo,
)

enum class PermissionStatus {
    DeniedPermanently,
    DeniedTemporarily,
    Granted,
    Pending,
    Unknown,
}

sealed interface WifiScreenEvent {
    data object LaunchFineLocation : WifiScreenEvent
}
