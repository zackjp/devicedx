package com.zackjp.devicedx.feature.dashboard

data class DashboardScreenState(
    val wifiNames: List<String>
)


sealed interface DashboardEvent {
    data object LaunchFineLocation : DashboardEvent
}