package com.zackjp.devicedx.feature.wifi

data class WifiScreenState(
    val isMonitorActive: Boolean,
    val permissionStatus: PermissionStatus,
    val wifiNames: List<String>,
    val wifiStrength: Int,
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
