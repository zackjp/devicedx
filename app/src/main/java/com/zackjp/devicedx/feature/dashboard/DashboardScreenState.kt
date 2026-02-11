package com.zackjp.devicedx.feature.dashboard

data class DashboardScreenState(
    val permissionStatus: PermissionStatus,
    val wifiNames: List<String>,
)

enum class PermissionStatus {
    Denied,
    Granted,
    Pending,
    Unknown,
}

sealed interface DashboardEvent {
    data object LaunchFineLocation : DashboardEvent
}