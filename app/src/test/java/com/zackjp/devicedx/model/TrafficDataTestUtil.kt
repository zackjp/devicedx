package com.zackjp.devicedx.model


fun TrafficSession.Companion.fake(
    number: Long,
    metricsCount: Int,
) = TrafficSession(
    id = number,
    startTime = number * 1000L,
    trafficMetrics = List(metricsCount) { TrafficMetric.fake(it.toLong()) }
)

fun TrafficData.Companion.fake(number: Long): TrafficData = TrafficData(
    timestamp = number * 1000L,
    rxBytes = number * 100 + 1,
    txBytes = number * 100 + 2,
)

fun TrafficMetric.Companion.fake(number: Long): TrafficMetric = TrafficMetric(
    timestamp = number * 1000L,
    rxBytesPerSec = number * 100 + 1,
    txBytesPerSec = number * 100 + 2,
)
