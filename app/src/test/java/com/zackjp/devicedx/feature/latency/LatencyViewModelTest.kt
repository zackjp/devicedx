package com.zackjp.devicedx.feature.latency

import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.feature.latency.LatencyViewModel.Companion.MAX_LATENCY_DATA_POINTS
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


@OptIn(ExperimentalCoroutinesApi::class) // Dispatchers.setMain()/resetMain(), advanceUntilIdle()
class LatencyViewModelTest {


    private val testDispatcherProvider = TestDispatcherProvider()
    private val realTimeNetworkDataSource = mockk<RealTimeNetworkDataSource>()

    private val latencyMillisFlow = MutableSharedFlow<Long>()

    private lateinit var viewModel: LatencyViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcherProvider.default)

        every { realTimeNetworkDataSource.getLatencyMillisFlow() } returns latencyMillisFlow
        viewModel = LatencyViewModel(
            dispatcherProvider = testDispatcherProvider,
            realTimeNetworkDataSource = realTimeNetworkDataSource,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startMonitor_SetsMonitorActiveToTrue() = runTest {
        initViewModel()

        viewModel.screenState.test {
            awaitItem().isMonitorActive shouldBe false

            viewModel.startMonitor()
            advanceUntilIdle()

            awaitItem().isMonitorActive shouldBe true
        }
    }

    @Test
    fun startMonitor_WhenLatencyValueEmitted_UpdatesLatencyHistory() = runTest {
        initViewModel()

        viewModel.screenState.test {
            skipItems(1) // initial state
            viewModel.startMonitor()
            advanceUntilIdle()

            latencyMillisFlow.emit(11L)
            advanceUntilIdle()

            expectMostRecentItem().latencyHistory shouldBe listOf(11L)

            latencyMillisFlow.emit(13L)
            advanceUntilIdle()

            expectMostRecentItem().latencyHistory shouldBe listOf(11L, 13L)
        }
    }

    @Test
    fun startMonitor_WhenLatencyValueEmitted_OnlyKeepsMaxHistory() = runTest {
        initViewModel()

        viewModel.screenState.test {
            skipItems(1) // initial state
            viewModel.startMonitor()
            advanceUntilIdle()

            repeat(MAX_LATENCY_DATA_POINTS + 3) {
                latencyMillisFlow.emit(it.toLong())
                advanceUntilIdle()
            }

            val expectedHistory = (3..<3 + MAX_LATENCY_DATA_POINTS)
                .map { it.toLong() }
                .toList()
            expectMostRecentItem().latencyHistory shouldBe expectedHistory
        }
    }

    @Test
    fun startMonitor_WhenScreenStateHasZeroSubscribers_AutoPausesEmissions() = runTest {
        // Manually subscribe and don't use Turbine, which would also count as a subscriber
        val uiEmulatedSubscription = viewModel.screenState.launchIn(backgroundScope)
        val expectedTimeoutMs = 5000L
        advanceUntilIdle()

        viewModel.startMonitor()
        advanceUntilIdle()

        // 1) Cancel "ui" subscription and move time up until 1ms before WhileSubscribed times out
        uiEmulatedSubscription.cancel()
        advanceTimeBy(expectedTimeoutMs - 1)

        // 2) Emit data that should still propagate to screen state
        latencyMillisFlow.emit(123L)
        runCurrent()
        viewModel.screenState.value.latencyHistory shouldBe listOf(123L)

        // 3) Advance 1 more millisecond to force the WhileSubscribed timeout
        advanceTimeBy(1)

        // 4) Try to emit new data, which should not work
        latencyMillisFlow.emit(456L)
        runCurrent()
        viewModel.screenState.value.latencyHistory shouldBe listOf(123L)
    }

    @Test
    fun stopMonitor_SetsMonitorActiveToFalse() = runTest {
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
    fun stopMonitor_WhenMonitorActive_StopsNewEmissions() = runTest {
        initViewModel()

        viewModel.screenState.test {
            viewModel.startMonitor()
            advanceUntilIdle()
            expectMostRecentItem().latencyHistory shouldBe emptyList()

            latencyMillisFlow.emit(11L)
            advanceUntilIdle()
            expectMostRecentItem().latencyHistory shouldBe listOf(11L)

            viewModel.stopMonitor()
            advanceUntilIdle()
            latencyMillisFlow.emit(13L)
            advanceUntilIdle()
            expectMostRecentItem().latencyHistory shouldBe listOf(11L)
        }
    }

    private fun TestScope.initViewModel() {
        viewModel.screenState.launchIn(backgroundScope)
        advanceUntilIdle()
    }

}