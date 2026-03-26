package com.zackjp.devicedx.feature.latency

import androidx.compose.runtime.Immutable

@Immutable
data class LatencyScreenState(
    val isMonitorActive: Boolean,
    val latencyHistory: List<Long>,
)
