package com.zackjp.devicedx.model

data class TrafficData(
    val timestamp: Long,
    val rxBytes: Long,
    val txBytes: Long,
) {
    companion object
}

data class TrafficMetric(
    val timestamp: Long,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
)