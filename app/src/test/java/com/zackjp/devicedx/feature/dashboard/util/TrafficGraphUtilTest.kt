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
    fun calculateMetrics_WithSingleDataPoint_MeasuresZeroRxPerSec() {
        graphUtil.calculateMetrics(
            listOf(TrafficData.fake(1)),
            1234L,
            1.seconds,
        ) shouldContainExactly listOf(
            TrafficMetric(1000, 0f),
        )
    }

    @Test
    fun calculateMetrics_WithMultipleDataPoints_MeasuresCorrectRxPerSec() {
        val partialBucketTime = 3500L
        graphUtil.calculateMetrics(
            listOf(
                TrafficData(timestamp = 1000, rxBytes = 5),
                TrafficData(timestamp = 2000, rxBytes = 7),
                TrafficData(timestamp = 3000, rxBytes = 11),
                TrafficData(timestamp = partialBucketTime, rxBytes = 17), // should be ignored
            ),
            partialBucketTime + 1,
            3.seconds,
        ) shouldContainExactly listOf(
            TrafficMetric(1000, 0f),
            TrafficMetric(2000, 2f),
            TrafficMetric(3000, 4f),
        )
    }

    @Test
    fun calculateMetrics_WithSporadicDataPoints_MeasuresCorrectRxPerSec() {
        graphUtil.calculateMetrics(
            listOf(
                TrafficData(timestamp = 2000, rxBytes = 5),
                TrafficData(timestamp = 4000, rxBytes = 7),
                TrafficData(timestamp = 5000, rxBytes = 11),
            ),
            5500L,
            5.seconds,
        ) shouldContainExactly listOf(
            TrafficMetric(1000, 0f),
            TrafficMetric(2000, 0f),
            TrafficMetric(3000, 0f),
            TrafficMetric(4000, 0f),
            TrafficMetric(5000, 4f),
        )
    }

    @Test
    fun calculateMetrics_WithMultiBucketedDataPoints_MeasuresRxPerSecUsingLatestTimestampInBucket() {
        graphUtil.calculateMetrics(
            listOf(
                TrafficData(timestamp = 250, rxBytes = 5), // in order
                TrafficData(timestamp = 750, rxBytes = 7), // in order. should be used in calc
                TrafficData(timestamp = 1400, rxBytes = 13), // out of order. should be used in calc
                TrafficData(timestamp = 1300, rxBytes = 11), // out of order
                TrafficData(timestamp = 2500, rxBytes = 23), // single bucket value
            ),
            3000L,
            3.seconds,
        ) shouldContainExactly listOf(
            TrafficMetric(1000, 0f),
            TrafficMetric(2000, 6f),
            TrafficMetric(3000, 10f),
        )
    }

}
