package com.zackjp.devicedx.feature.traffic

import androidx.compose.runtime.Immutable
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession


@Immutable
data class TrafficScreenState(
    val graphData: List<TrafficMetric>,
    val isMonitorActive: Boolean,
    val sessionStartTime: Long?,
    val trafficSession: TrafficSession?,
)
