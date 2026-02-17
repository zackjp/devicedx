package com.zackjp.devicedx.navigation

data class NavActions(
    val toDashboard: () -> Unit,
    val toTrafficMonitor: () -> Unit,
)