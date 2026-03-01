package com.zackjp.devicedx.feature.dashboard.util

import com.zackjp.devicedx.feature.traffic.util.TrafficGraphUtil
import com.zackjp.devicedx.model.TrafficData
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.fake
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TrafficGraphUtilTest {

    private lateinit var graphUtil: TrafficGraphUtil

    @BeforeEach
    fun setUp() {
        graphUtil = TrafficGraphUtil()
    }

    @Test
    fun calculateMetrics_WhenDataIsEmpty_ReturnsEmptyList() {
        graphUtil.calculateMetrics(
            emptyList(),
            1234L,
            100.milliseconds,
        ) shouldBe emptyList()
    }

    @Test
    fun calculateMetrics_WithSingleDataPoint_MeasuresZeroRxAndTxPerSec() {
        graphUtil.calculateMetrics(
            listOf(TrafficData.fake(1)),
            1234L,
            1.seconds,
        ) shouldContainExactly listOf(
            TrafficMetric(timestamp = 1000, rxBytesPerSec = 0, txBytesPerSec = 0),
        )
    }

    @Test
    fun calculateMetrics_WithMultipleDataPoints_MeasuresCorrectRxAndTxPerSec() {
        val partialBucketTime = 3500L
        graphUtil.calculateMetrics(
            listOf(
                TrafficData(timestamp = 1000, rxBytes = 5, txBytes = 101),
                TrafficData(timestamp = 2000, rxBytes = 7, txBytes = 214),
                TrafficData(timestamp = 3000, rxBytes = 11, txBytes = 300),
                TrafficData( // should be ignored
                    timestamp = partialBucketTime,
                    rxBytes = 17,
                    txBytes = 117
                ),
            ),
            partialBucketTime + 1,
            3.seconds,
        ) shouldContainExactly listOf(
            TrafficMetric(timestamp = 1000, rxBytesPerSec = 0, txBytesPerSec = 0),
            TrafficMetric(timestamp = 2000, rxBytesPerSec = 2, txBytesPerSec = 113),
            TrafficMetric(timestamp = 3000, rxBytesPerSec = 4, txBytesPerSec = 86),
        )
    }

    @Test
    fun calculateMetrics_WithSporadicDataPoints_MeasuresCorrectRxAndTxPerSec() {
        graphUtil.calculateMetrics(
            listOf(
                // skip timestamp = 1000
                TrafficData(timestamp = 2000, rxBytes = 5, txBytes = 101),
                // skip timestamp = 3000
                TrafficData(timestamp = 4000, rxBytes = 7, txBytes = 214),
                TrafficData(timestamp = 5000, rxBytes = 11, txBytes = 300),
            ),
            5500L,
            5.seconds,
        ) shouldContainExactly listOf(
            TrafficMetric(timestamp = 1000, rxBytesPerSec = 0, txBytesPerSec = 0),
            TrafficMetric(timestamp = 2000, rxBytesPerSec = 0, txBytesPerSec = 0),
            TrafficMetric(timestamp = 3000, rxBytesPerSec = 0, txBytesPerSec = 0),
            TrafficMetric(timestamp = 4000, rxBytesPerSec = 0, txBytesPerSec = 0),
            TrafficMetric(timestamp = 5000, rxBytesPerSec = 4, txBytesPerSec = 86),
        )
    }

    @Test
    fun calculateMetrics_WithMultiBucketedDataPoints_MeasuresRxTxUsingLatestTimestampInBucket() {
        graphUtil.calculateMetrics(
            listOf(
                // in chronological order:
                TrafficData(timestamp = 250, rxBytes = 5, txBytes = 102),

                // in chronological order. should be used in calc:
                TrafficData(timestamp = 750, rxBytes = 7, txBytes = 208),

                // out of chronological order. should be used in calc:
                TrafficData(timestamp = 1400, rxBytes = 13, txBytes = 320),

                // out of chronological order + not latest in bucket. not used in calc:
                TrafficData(timestamp = 1300, rxBytes = 11, txBytes = 300),

                // single bucket value:
                TrafficData(timestamp = 2500, rxBytes = 23, txBytes = 400),
            ),
            3000L,
            3.seconds,
        ) shouldContainExactly listOf(
            TrafficMetric(timestamp = 1000, rxBytesPerSec = 0, txBytesPerSec = 0),
            TrafficMetric(timestamp = 2000, rxBytesPerSec = 6, txBytesPerSec = 112),
            TrafficMetric(timestamp = 3000, rxBytesPerSec = 10, txBytesPerSec = 80),
        )
    }

}
