package com.zackjp.devicedx.navigation

import androidx.navigation3.runtime.NavKey

interface Route {
    data object Dashboard : Route, NavKey
    data object TrafficMonitor : Route, NavKey
}
