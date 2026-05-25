package com.zackjp.devicedx.feature.traffic.model

import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficSession
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.RoundingMode
import kotlin.math.pow

class TrafficFeatureModelTest {

    @Test
    fun computeDisplayInfo_ReturnsDisplayInfoWithCorrectSession() {
        val trafficSession = TrafficSession(
            id = 1234L,
            startTime = 456L,
            endTime = 789L,
            totalRxBytes = 0,
            totalTxBytes = 0,
            trafficMetrics = emptyList(),
        )

        val displayInfo = trafficSession.computeDisplayInfo()

        displayInfo.session shouldBe trafficSession
    }

    @Test
    fun computeDisplayInfo_ReturnsDisplayInfoWithCorrectDisplayableUnits() {
        val kb = 1024L
        val mb = 1024.0.pow(2).toLong()
        val trafficSession = TrafficSession(
            id = 1234L,
            startTime = 456L,
            endTime = 789L,
            totalRxBytes = kb + (kb / 4),
            totalTxBytes = mb + (mb / 2),
            trafficMetrics = emptyList(),
        )

        val displayInfo = trafficSession.computeDisplayInfo()

        displayInfo.totalRxValue shouldBe 1.25.toBigDecimal()
        displayInfo.totalRxUnit shouldBe DataUnit.KILOBYTE
        displayInfo.totalTxValue shouldBe 1.50.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        displayInfo.totalTxUnit shouldBe DataUnit.MEGABYTE
    }

}