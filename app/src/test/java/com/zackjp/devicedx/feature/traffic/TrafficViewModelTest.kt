package com.zackjp.devicedx.feature.traffic

import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.TrafficRepository
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession
import com.zackjp.devicedx.model.fake
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle()
class TrafficViewModelTest {

    private val testDispatcherProvider = TestDispatcherProvider()

    private val clock = mockk<Clock>()
    private val trafficRepository = mockk<TrafficRepository>()

    private val trafficSessionFlow = MutableSharedFlow<TrafficSession>()

    private lateinit var viewModel: TrafficViewModel

    val metricsSession1 = TrafficSession.fake(number = 100003, metricsCount = 2)
    val metricsSession2 = TrafficSession.fake(number = 100007, metricsCount = 2)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcherProvider.default)

        every { clock.now() } returns Instant.fromEpochMilliseconds(10)
        every { trafficRepository.recordTrafficMetrics() } returns trafficSessionFlow

        viewModel = TrafficViewModel(
            clock = clock,
            trafficRepository = trafficRepository
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startMonitor_WhenScreenStateReachesZeroSubscribers_AutoPausesEmissions() = runTest {
        // Manually subscribe and don't use Turbine, which would also count as a subscriber
        val uiEmulatedSubscription = viewModel.screenState.launchIn(backgroundScope)
        advanceUntilIdle()
        val expectedTimeoutMs = 5000L

        val sessionThatShouldEmit = metricsSession1
        val sessionThatShouldNotEmit = metricsSession2

        every { clock.now() } returns Instant.fromEpochMilliseconds(3333L)

        viewModel.startMonitor()
        advanceUntilIdle()
        viewModel.screenState.value.trafficSession should beNull()

        // 1) Cancel "ui" subscription and move time up until 1ms before WhileSubscribed times out
        uiEmulatedSubscription.cancel()
        advanceTimeBy(expectedTimeoutMs - 1)

        // 2) Emit data that should still generate metrics
        trafficSessionFlow.emit(sessionThatShouldEmit)
        runCurrent() // use runCurrent so it doesn't advance the clock
        viewModel.screenState.value.trafficSession shouldBe sessionThatShouldEmit

        // 3) Advance 1 more millisecond to force the WhileSubscribed timeout
        advanceTimeBy(1)
        runCurrent()

        // 4) Try to emit new data, which should not work
        trafficSessionFlow.emit(sessionThatShouldNotEmit)
        runCurrent()
        viewModel.screenState.value.trafficSession shouldBe sessionThatShouldEmit
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
    fun startMonitor_SetsSessionStartTimeToCurrentTime() = runTest {
        initViewModel()

        every { clock.now() } returns Instant.fromEpochMilliseconds(27)

        viewModel.screenState.test {
            expectMostRecentItem().sessionStartTime shouldBe null

            viewModel.startMonitor()
            advanceUntilIdle()

            expectMostRecentItem().sessionStartTime shouldBe 27
        }
    }

    @Test
    fun startMonitor_WhenTrafficDataEmitted_UpdatesHistory() = runTest {
        initViewModel()

        val expectedSession = metricsSession1
        val expectedClockTime = expectedSession.maxTrafficTimestamp()
        every { clock.now() } returns Instant.fromEpochMilliseconds(expectedClockTime)

        viewModel.screenState.test {
            expectMostRecentItem().trafficSession should beNull()

            viewModel.startMonitor()
            advanceUntilIdle()

            trafficSessionFlow.emit(expectedSession)
            advanceUntilIdle()

            expectMostRecentItem().trafficSession shouldBe expectedSession
        }
    }

    @Test
    fun stopMonitoring_WhenMonitorActive_StopsNewEmissions() = runTest {
        initViewModel()

        val session1 = metricsSession1
        val session2 = metricsSession2

        every { clock.now() } returns Instant.fromEpochMilliseconds(session1.maxTrafficTimestamp())

        viewModel.screenState.test {
            viewModel.startMonitor()
            advanceUntilIdle()
            expectMostRecentItem().trafficSession should beNull()

            trafficSessionFlow.emit(session1)
            advanceUntilIdle()
            expectMostRecentItem().trafficSession shouldBe session1

            viewModel.stopMonitor()
            advanceUntilIdle()

            trafficSessionFlow.emit(session2)
            advanceUntilIdle()
            expectMostRecentItem().trafficSession shouldBe session1
        }
    }

    @Test
    fun stopMonitor_WhenSessionStartTimeIsNonNull_ShouldNotResetTheStartTime() = runTest {
        initViewModel()

        viewModel.screenState.test {
            every { clock.now() } returns Instant.fromEpochMilliseconds(19)
            viewModel.startMonitor()
            advanceUntilIdle()
            expectMostRecentItem().sessionStartTime shouldBe 19

            every { clock.now() } returns Instant.fromEpochMilliseconds(51)
            viewModel.stopMonitor()
            advanceUntilIdle()
            expectMostRecentItem().sessionStartTime shouldBe 19
        }
    }

    private fun TestScope.initViewModel() {
        viewModel.screenState.launchIn(backgroundScope)
    }

}

private fun TrafficSession.maxTrafficTimestamp(): Long =
    trafficMetrics.maxOf { it.timestamp }
