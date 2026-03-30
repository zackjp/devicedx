package com.zackjp.devicedx.data

import app.cash.turbine.test
import com.zackjp.devicedx.model.TrafficData
import com.zackjp.devicedx.network.NetworkUtility
import com.zackjp.devicedx.network.TrafficStatsWrapper
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class RealTimeNetworkDataSourceTest {

    private val testDispatcher = StandardTestDispatcher()
    private val networkUtility = mockk<NetworkUtility>()
    private val trafficStatsWrapper = mockk<TrafficStatsWrapper>()
    private val clock = mockk<Clock>()

    private fun TestScope.buildDataSource() = RealTimeNetworkDataSource(
        networkUtility = networkUtility,
        clock = clock,
        appScope = backgroundScope,
        trafficStatsWrapper = trafficStatsWrapper,
    )

    @Test
    fun getLatencyMillisFlow_WhenCalculateLatencySucceeds_EmitsValue() = runTest(testDispatcher) {
        coEvery { networkUtility.calculateLatency() } returns 100L

        buildDataSource().getLatencyMillisFlow().test {
            advanceUntilIdle()
            awaitItem() shouldBe 100L
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLatencyMillisFlow_WhenCalculateLatencyReturnsNegativeOne_FiltersEmission() = runTest(testDispatcher) {
        coEvery { networkUtility.calculateLatency() } returnsMany listOf(-1L, 50L)

        buildDataSource().getLatencyMillisFlow().test {
            advanceUntilIdle()
            awaitItem() shouldBe 50L
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getTrafficStats_WhenWrapperReturnsBytes_EmitsTrafficData() = runTest(testDispatcher) {
        val fixedTimestamp = 1234567890L
        every { clock.now() } returns Instant.fromEpochMilliseconds(fixedTimestamp)
        every { trafficStatsWrapper.getTotalRxBytes() } returns 1000L
        every { trafficStatsWrapper.getTotalTxBytes() } returns 2000L

        buildDataSource().getTrafficStats().test {
            advanceUntilIdle()
            awaitItem() shouldBe TrafficData(
                timestamp = fixedTimestamp,
                rxBytes = 1000L,
                txBytes = 2000L,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getTrafficStats_WhenWrapperThrows_ExceptionPropagatesToAppScope() = runTest(testDispatcher) {
        val expectedException = RuntimeException("TrafficStats unavailable")
        every { clock.now() } returns Instant.fromEpochMilliseconds(0L)
        every { trafficStatsWrapper.getTotalRxBytes() } throws expectedException

        var caughtException: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, throwable -> caughtException = throwable }
        val appScope = CoroutineScope(testDispatcher + exceptionHandler)
        val dataSource = RealTimeNetworkDataSource(
            networkUtility = networkUtility,
            clock = clock,
            appScope = appScope,
            trafficStatsWrapper = trafficStatsWrapper,
        )

        dataSource.getTrafficStats().launchIn(appScope)
        advanceUntilIdle()

        caughtException shouldBe expectedException
    }
}
