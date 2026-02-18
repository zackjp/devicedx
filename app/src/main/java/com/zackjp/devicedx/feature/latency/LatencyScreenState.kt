package com.zackjp.devicedx.feature.latency

data class LatencyScreenState(
    val isMonitorActive: Boolean,
    val latencyHistory: List<Long>,
)
