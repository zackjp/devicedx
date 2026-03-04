package com.zackjp.devicedx.feature.traffic

import com.zackjp.devicedx.model.TrafficMetric

data class TrafficScreenState(
    val isMonitorActive: Boolean,
    val sessionStartTime: Long?,
    val trafficMetrics: List<TrafficMetric>,
)
