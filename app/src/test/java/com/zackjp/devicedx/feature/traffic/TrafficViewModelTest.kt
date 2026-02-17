package com.zackjp.devicedx.feature.traffic

import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.feature.dashboard.util.TrafficGraphUtil
import com.zackjp.devicedx.feature.traffic.TrafficViewModel.Companion.TRAFFIC_METRICS_WINDOW_SECS
import com.zackjp.devicedx.model.TrafficData
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.fake
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle()
class TrafficViewModelTest {

    private val testDispatcherProvider = TestDispatcherProvider()

    private val clock = mockk<Clock>()
    private val realTimeNetworkDataSource = mockk<RealTimeNetworkDataSource>()
    private val trafficGraphUtil = mockk<TrafficGraphUtil>()

    private val trafficStatsFlow = MutableSharedFlow<TrafficData>()

    private lateinit var viewModel: TrafficViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcherProvider.default)

        every { clock.now() } returns Instant.fromEpochMilliseconds(10)
        every { realTimeNetworkDataSource.getTrafficStats() } returns trafficStatsFlow
        every { trafficGraphUtil.calculateMetrics(any(), any(), any()) } returns emptyList()

        viewModel = TrafficViewModel(
            clock = clock,
            dispatcherProvider = testDispatcherProvider,
            realTimeNetworkDataSource = realTimeNetworkDataSource,
            trafficGraphUtil = trafficGraphUtil,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stopMonitoring_WhenMonitorActive_StopsNewEmissions() = runTest {
        initViewModel()

        val expectedMetrics = listOf(TrafficMetric(11, 22f), TrafficMetric(33, 44f))
        val unexpectedMetrics = listOf(TrafficMetric(55, 66f), TrafficMetric(77, 88f))

        every { trafficGraphUtil.calculateMetrics(any(), any(), any()) } returns emptyList()
        every { clock.now() } returns Instant.fromEpochMilliseconds(1234)

        viewModel.screenState.test {
            viewModel.startMonitor()
            advanceUntilIdle()
            expectMostRecentItem().trafficMetrics shouldBe emptyList()

            every { trafficGraphUtil.calculateMetrics(any(), any(), any()) } returns expectedMetrics
            trafficStatsFlow.emit(TrafficData.fake(1))
            advanceUntilIdle()
            expectMostRecentItem().trafficMetrics shouldBe expectedMetrics

            every {
                trafficGraphUtil.calculateMetrics(any(), any(), any())
            } returns unexpectedMetrics

            viewModel.stopMonitor()
            advanceUntilIdle()

            trafficStatsFlow.emit(TrafficData.fake(1))
            advanceUntilIdle()
            expectMostRecentItem().trafficMetrics shouldBe expectedMetrics
        }
    }

    @Test
    fun startMonitor_SetsActiveToTrue() = runTest {
        initViewModel()

        viewModel.screenState.test {
            expectMostRecentItem().isMonitorActive shouldBe false

            viewModel.startMonitor()
            advanceUntilIdle()

            expectMostRecentItem().isMonitorActive shouldBe true
        }
    }

    @Test
    fun stopMonitor_SetsActiveToFalse() = runTest {
        initViewModel()

        viewModel.screenState.test {
            viewModel.startMonitor()
            advanceUntilIdle()

            expectMostRecentItem().isMonitorActive shouldBe true

            viewModel.stopMonitor()
            advanceUntilIdle()

            expectMostRecentItem().isMonitorActive shouldBe false
        }
    }

    @Test
    fun startMonitorTraffic_WhenTrafficDataEmitted_UpdatesHistory() = runTest {
        initViewModel()

        val dataA = TrafficData.fake(1)
        val dataB = TrafficData.fake(3)
        val dataC = TrafficData.fake(5)
        val expectedMetrics = listOf(TrafficMetric(111, 222f), TrafficMetric(333, 444f))
        val expectedClockTime = 12345L
        every { clock.now() } returns Instant.fromEpochMilliseconds(expectedClockTime)
        // define the more general mock first
        every { trafficGraphUtil.calculateMetrics(any(), any(), any()) } returns emptyList()
        every {
            trafficGraphUtil.calculateMetrics(
                listOf(dataA, dataB, dataC), // this will trigger on the last data emission
                expectedClockTime,
                TRAFFIC_METRICS_WINDOW_SECS.seconds
            )
        } returns expectedMetrics

        viewModel.screenState.test {
            expectMostRecentItem().trafficMetrics shouldBe emptyList()

            viewModel.startMonitor()
            advanceUntilIdle()

            trafficStatsFlow.emit(dataA)
            advanceUntilIdle()
            trafficStatsFlow.emit(dataB)
            advanceUntilIdle()
            trafficStatsFlow.emit(dataC)
            advanceUntilIdle()

            expectMostRecentItem().trafficMetrics shouldBe expectedMetrics
        }
    }

    private fun TestScope.initViewModel() {
        viewModel.screenState.launchIn(backgroundScope)
    }

}