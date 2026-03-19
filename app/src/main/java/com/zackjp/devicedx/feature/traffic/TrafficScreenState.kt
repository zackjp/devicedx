package com.zackjp.devicedx.feature.traffic

import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession

data class TrafficScreenState(
    val graphData: List<TrafficMetric>,
    val isMonitorActive: Boolean,
    val sessionStartTime: Long?,
    val trafficSession: TrafficSession?,
)
