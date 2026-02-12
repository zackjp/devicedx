package com.zackjp.devicedx.feature.dashboard

data class DashboardScreenState(
    val activeView: DashboardView,
    val latencyMillis: List<Long>,
    val permissionStatus: PermissionStatus,
    val wifiNames: List<String>,
)

enum class DashboardView {
    Unselected,
    Wifi,
    Latency
}

enum class PermissionStatus {
    Denied,
    Granted,
    Pending,
    Unknown,
}

sealed interface DashboardEvent {
    data object LaunchFineLocation : DashboardEvent
}