package com.zackjp.devicedx.navigation

data class NavActions(
    val toDashboard: () -> Unit,
    val toLatencyMonitor: () -> Unit,
    val toTrafficMonitor: () -> Unit,
    val toTrafficHistory: () -> Unit,
    val toWifiMonitor: () -> Unit,
)