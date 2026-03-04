package com.zackjp.devicedx.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

interface Route {
    @Serializable
    data object Dashboard : Route, NavKey
    @Serializable
    data object TrafficMonitor : Route, NavKey
    @Serializable
    data object LatencyMonitor : Route, NavKey
    @Serializable
    data object WifiMonitor : Route, NavKey
}
