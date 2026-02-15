package com.zackjp.devicedx.feature.dashboard

import com.zackjp.devicedx.model.TrafficMetric

data class DashboardScreenState(
    val activeView: DashboardView,
    val latencyHistory: List<Long>,
    val permissionStatus: PermissionStatus,
    val trafficMetrics: List<TrafficMetric>,
    val wifiNames: List<String>,
)

enum class DashboardView {
    Unselected,
    Wifi,
    Latency,
    Traffic
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