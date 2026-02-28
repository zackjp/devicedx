package com.zackjp.devicedx.feature.traffic.util

import com.zackjp.devicedx.model.TrafficData
import com.zackjp.devicedx.model.TrafficMetric
import javax.inject.Inject
import kotlin.time.Duration

class TrafficGraphUtil @Inject constructor() {

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
                TrafficMetric(currentSec, 0L)
            } else {
                TrafficMetric(
                    currentSec,
                    (currentSecData.rxBytes - priorSecData.rxBytes).coerceAtLeast(0)
                )
            }
        }
    }

}
