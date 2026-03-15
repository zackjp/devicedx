package com.zackjp.devicedx.feature.traffic.util

import com.zackjp.devicedx.model.TrafficData
import com.zackjp.devicedx.model.TrafficMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import javax.inject.Inject
import kotlin.time.Duration


class TrafficGraphUtil @Inject constructor() {

    fun runningMetricsCalculation(flow: Flow<TrafficData>): Flow<TrafficMetric> =
        flow.scan(emptyMap<Long, TrafficData>()) { accumulator, value ->
            // Minus 1ms to have an exact second count towards itself (eg, 3000-1 -> 3000)
            val bucket = (value.timestamp - 1) / 1000 * 1000 + 1000

            // Ignore additional data points for the same time bucket, if necessary. This could
            // happen if TrafficStats are emitted more than once in the same time bucket.
            val bucketedData = if (accumulator.containsKey(bucket)) {
                accumulator
            } else {
                accumulator + (bucket to value)
            }

            // Drop the earliest timestamp, if necessary. We only want 2 data points
            // to calculate deltas.
            if (bucketedData.size > 2) {
                val firstKey = bucketedData.keys.first()
                bucketedData - firstKey
            } else {
                bucketedData
            }
        }
            .distinctUntilChanged() // Don't re-record bucketed data that hasn't changed
            .mapNotNull { bucketedTrafficData ->
                if (bucketedTrafficData.size != 2) {
                    return@mapNotNull null
                }
                val (_, bucket2) = bucketedTrafficData.entries.take(2)
                val data2 = bucket2.value
                val data1 = bucketedTrafficData[bucket2.key - 1000] // Get data for prior second

                val (rxBytesPerSec, txBytesPerSec) = if (data1 == null) {
                    0L to 0L
                } else {
                    (data2.rxBytes - data1.rxBytes) to (data2.txBytes - data1.txBytes)
                }

                return@mapNotNull TrafficMetric(
                    data2.timestamp,
                    rxBytesPerSec = rxBytesPerSec,
                    txBytesPerSec = txBytesPerSec,
                )
            }

    fun calculateMetrics(
        data: List<TrafficData>,
        endTime: Long,
        window: Duration,
    ): List<TrafficMetric> {
        if (data.isEmpty()) {
            return emptyList()
        }

        val bucketedDataBySecond = mutableMapOf<Long, TrafficData>()
        data.forEach { trafficData ->
            /*
             * +1000 so that partial millis counts towards the next bucket (eg, 1250ms -> 2000ms).
             * And -1 so an exact second always counts towards its own bucket (eg, 1000ms-1 -> 1000ms)
             */
            val bucket = (trafficData.timestamp - 1) / 1000 * 1000 + 1000

            bucketedDataBySecond.compute(bucket) { _, existingData ->
                when {
                    existingData == null -> trafficData
                    trafficData.timestamp > existingData.timestamp -> trafficData
                    else -> existingData
                }
            }
        }

        val endBucket = endTime / 1000 * 1000
        // +1000 to exclude start bucket, which doesn't have a prior data point to measure
        val startBucket = endBucket - (window.inWholeMilliseconds / 1000 * 1000) + 1000
        val range = startBucket..endBucket

        return range.step(1000).map { currentSec ->
            val priorSec = currentSec - 1000
            val currentSecData = bucketedDataBySecond[currentSec]
            val priorSecData = bucketedDataBySecond[priorSec]
            if (currentSecData == null || priorSecData == null) {
                TrafficMetric(
                    timestamp = currentSec,
                    rxBytesPerSec = 0L,
                    txBytesPerSec = 0L,
                )
            } else {
                TrafficMetric(
                    timestamp = currentSec,
                    rxBytesPerSec = (currentSecData.rxBytes - priorSecData.rxBytes).coerceAtLeast(0),
                    txBytesPerSec = (currentSecData.txBytes - priorSecData.txBytes).coerceAtLeast(0),
                )
            }
        }
    }

}
