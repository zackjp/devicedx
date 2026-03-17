package com.zackjp.devicedx.feature.traffic

import com.zackjp.devicedx.model.TrafficSession

data class TrafficScreenState(
    val isMonitorActive: Boolean,
    val sessionStartTime: Long?,
    val trafficSession: TrafficSession?,
)
