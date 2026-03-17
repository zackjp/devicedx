package com.zackjp.devicedx.model

data class TrafficSession(
    val id: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val totalRxBytes: Long = 0,
    val totalTxBytes: Long = 0,
    val trafficMetrics: List<TrafficMetric>,
) {
    companion object // used for test extensions
}

data class TrafficData(
    val timestamp: Long,
    val rxBytes: Long,
    val txBytes: Long,
) {
    companion object // used for test extensions
}

data class TrafficMetric(
    val timestamp: Long,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
) {
    companion object // used for test extensions
}