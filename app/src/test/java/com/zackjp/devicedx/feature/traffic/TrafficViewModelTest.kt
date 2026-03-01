package com.zackjp.devicedx.feature.traffic

import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.feature.traffic.TrafficViewModel.Companion.TRAFFIC_METRICS_WINDOW_SECS
import com.zackjp.devicedx.feature.traffic.util.TrafficGraphUtil
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

    val metrics1 = listOf(
        TrafficMetric(timestamp = 11, rxBytesPerSec = 22, txBytesPerSec = 33),
        TrafficMetric(timestamp = 44, rxBytesPerSec = 55, txBytesPerSec = 66)
    )
    val metrics2 = listOf(
        TrafficMetric(timestamp = 987, rxBytesPerSec = 876, txBytesPerSec = 765),
        TrafficMetric(timestamp = 654, rxBytesPerSec = 543, txBytesPerSec = 432)
    )

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

        val expectedMetrics = metrics1
        val unexpectedMetrics = metrics2

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
    fun startMonitor_WhenScreenStateReachesZeroSubscribers_AutoPausesEmissions() = runTest {
        // Manually subscribe and don't use Turbine, which would also count as a subscriber
        val uiEmulatedSubscription = viewModel.screenState.launchIn(backgroundScope)
        advanceUntilIdle()
        val expectedTimeoutMs = 5000L

        val firstMetrics = metrics1
        val secondMetrics = metrics2

        every { trafficGraphUtil.calculateMetrics(any(), any(), any()) } returns emptyList()
        every { clock.now() } returns Instant.fromEpochMilliseconds(1234)

        viewModel.startMonitor()
        advanceUntilIdle()
        viewModel.screenState.value.trafficMetrics shouldBe emptyList()

        // 1) Cancel "ui" subscription and move time up until 1ms before WhileSubscribed times out
        uiEmulatedSubscription.cancel()
        advanceTimeBy(expectedTimeoutMs - 1)

        // 2) Emit data that should still generate metrics
        every { trafficGraphUtil.calculateMetrics(any(), any(), any()) } returns firstMetrics
        trafficStatsFlow.emit(TrafficData.fake(1))
        runCurrent() // use runCurrent so it doesn't advance the clock
        viewModel.screenState.value.trafficMetrics shouldBe firstMetrics

        // 3) Advance 1 more millisecond to force the WhileSubscribed timeout
        every { trafficGraphUtil.calculateMetrics(any(), any(), any()) } returns secondMetrics
        advanceTimeBy(1)
        runCurrent()

        // 4) Try to emit new data, which should not work
        trafficStatsFlow.emit(TrafficData.fake(2))
        runCurrent()
        viewModel.screenState.value.trafficMetrics shouldBe firstMetrics
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
        val expectedMetrics = metrics1
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