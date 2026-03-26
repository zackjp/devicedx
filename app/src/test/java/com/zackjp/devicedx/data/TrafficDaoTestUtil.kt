package com.zackjp.devicedx.data


fun TrafficSessionWithMetrics.Companion.fake(
    number: Long,
    metricsCount: Int,
): TrafficSessionWithMetrics {
    val sessionEntity = TrafficSessionEntity.fake(number)

    return TrafficSessionWithMetrics(
        session = sessionEntity,
        metrics = List(metricsCount) {
            TrafficMetricEntity.fake(
                number = it.toLong(),
                sessionId = sessionEntity.sessionId
            )
        }
    )
}

fun TrafficSessionEntity.Companion.fake(number: Long) =
    TrafficSessionEntity(
        sessionId = number,
        startTime = number * 10_000 + 111,
        endTime = number * 10_000 + 777,
        totalRxBytes = number * 10_000 + 11,
        totalTxBytes = number * 1000 + 19
    )

fun TrafficMetricEntity.Companion.fake(number: Long, sessionId: Long) =
    TrafficMetricEntity(
        metricId = number,
        sessionId = sessionId,
        timestamp = number * 10_000 + number,
        rxBytesPerSec = number * 100 + 3,
        txBytesPerSec = number * 100 + 50
    )