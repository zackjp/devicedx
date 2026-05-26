package com.zackjp.devicedx.feature.traffic

import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.RecordingState
import com.zackjp.devicedx.data.TrafficRepository
import com.zackjp.devicedx.feature.traffic.model.computeDisplayInfo
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession
import com.zackjp.devicedx.model.fake
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle()
class TrafficViewModelTest {

    private val testDispatcherProvider = TestDispatcherProvider()
    private val trafficRepository = mockk<TrafficRepository>()

    private val recordingStateFlow = MutableStateFlow<RecordingState>(RecordingState.Idle)

    val repoSession1 = TrafficSession.fake(number = 100003, metricsCount = 2, sortDesc = true)
    val repoSessionById = TrafficSession.fake(number = SESSION_ID, metricsCount = 2, sortDesc = true)

    private companion object {
        const val SESSION_ID = 113355L
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcherProvider.default)

        every { trafficRepository.currentActiveSession } returns recordingStateFlow
        every { trafficRepository.startRecording() } just runs
        every { trafficRepository.stopRecording() } answers { recordingStateFlow.value = RecordingState.Idle }
        every { trafficRepository.getSessionById(SESSION_ID) } returns flowOf(repoSessionById)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stopMonitor_ClearsTheSession() = runTest {
        val viewModel = initViewModel()

        viewModel.screenState.test {
            recordingStateFlow.value = RecordingState.Active(repoSession1)
            advanceUntilIdle()

            expectMostRecentItem().trafficDisplayInfo?.session shouldBe repoSession1

            viewModel.stopMonitor()
            advanceUntilIdle()

            expectMostRecentItem().trafficDisplayInfo?.session should beNull()
        }
    }

    @Test
    fun stopMonitor_CallsRepositoryStopRecording() = runTest {
        val viewModel = initViewModel()

        viewModel.startMonitor()
        viewModel.stopMonitor()

        verify { trafficRepository.stopRecording() }
    }

    @Test
    fun startMonitor_WhenTrafficDataEmitted_UpdatesTrafficSession() = runTest {
        val viewModel = initViewModel()

        val repoSession = repoSession1

        viewModel.screenState.test {
            expectMostRecentItem().trafficDisplayInfo?.session should beNull()

            viewModel.startMonitor()
            advanceUntilIdle()

            recordingStateFlow.value = RecordingState.Active(repoSession)
            advanceUntilIdle()

            expectMostRecentItem().trafficDisplayInfo?.session shouldBe repoSession
        }
    }

    @Test
    fun startMonitor_WhenTrafficDataEmitted_ComputesTrafficDisplayInfo() = runTest {
        val viewModel = initViewModel()

        val repoSession = repoSession1

        viewModel.screenState.test {
            expectMostRecentItem().trafficDisplayInfo?.session should beNull()

            viewModel.startMonitor()
            advanceUntilIdle()

            recordingStateFlow.value = RecordingState.Active(repoSession)
            advanceUntilIdle()

            val expected = expectMostRecentItem().trafficDisplayInfo
            expected shouldNot beNull()

            val session = expected!!.session
            val expectedCalculation = session.computeDisplayInfo()
            expected.totalTxValue shouldNotBe 0
            expected.totalTxValue shouldBe expectedCalculation.totalTxValue
            expected.totalTxUnit shouldBe expectedCalculation.totalTxUnit
            expected.totalRxValue shouldNotBe 0
            expected.totalRxValue shouldBe expectedCalculation.totalRxValue
            expected.totalRxUnit shouldBe expectedCalculation.totalRxUnit
        }
    }

    @Test
    fun init_WhenRepositoryEmitsActiveSession_StartTimeMatchesSessionStartTime() = runTest {
        val viewModel = initViewModel()

        val session = repoSession1

        viewModel.screenState.test {
            recordingStateFlow.value = RecordingState.Active(session)
            advanceUntilIdle()

            expectMostRecentItem().trafficDisplayInfo?.session?.startTime shouldBe session.startTime
        }
    }

    @Test
    fun init_WhenSessionAlreadyActive_ReflectsActiveSession() = runTest {
        val session = TrafficSession.fake(number = 777, metricsCount = 0)
        recordingStateFlow.value = RecordingState.Active(session)

        val viewModel = initViewModel()

        viewModel.screenState.test {
            advanceUntilIdle()

            expectMostRecentItem().trafficDisplayInfo?.session shouldBe session
        }
    }

    @Test
    fun init_WhenCreatedWithSessionId_LoadsSessionById() = runTest {
        val viewModel = initViewModel(sessionId = repoSessionById.id)

        viewModel.screenState.test {
            awaitItem().trafficDisplayInfo?.session shouldBe null

            runCurrent()
            awaitItem().trafficDisplayInfo?.session shouldBe repoSessionById
        }
    }

    @Test
    fun startMonitor_WhenTrafficMetricsEmitted_FiltersLastXSecondsBeforeFinalMetric() = runTest {
        val viewModel = initViewModel()

        val expectedFilterWindow = 30_000L
        val currentTime = 50_000L

        val belowWindowMetric = TrafficMetric.fake(101)
            .copy(timestamp = currentTime - expectedFilterWindow - 1000)
        val onWindowMetric = TrafficMetric.fake(201)
            .copy(timestamp = currentTime - expectedFilterWindow)
        val aboveWindowMetric = TrafficMetric.fake(301)
            .copy(timestamp = currentTime - expectedFilterWindow + 1000)
        val latestTimeMetric = TrafficMetric.fake(401)
            .copy(timestamp = currentTime)
        val session = TrafficSession.fake(number = 10003, metricsCount = 0).copy(
            // Specify metrics ourselves here in descending order to match TrafficRepo's
            // contract and to explicitly verify filtering in this unit test
            trafficMetrics = listOf(
                latestTimeMetric,
                aboveWindowMetric,
                onWindowMetric,
                belowWindowMetric,
            )
        )
        // Then ensure the filtered data is in ascending order for the View
        val expectedMetrics = listOf(onWindowMetric, aboveWindowMetric, latestTimeMetric)

        viewModel.screenState.test {
            viewModel.startMonitor()
            advanceUntilIdle()

            recordingStateFlow.value = RecordingState.Active(session)
            advanceUntilIdle()

            expectMostRecentItem().graphData shouldBe expectedMetrics
        }
    }

    @Test
    fun startMonitor_WhenThrowsException_UpdatesErrorState() = runTest {
        val viewModel = initViewModel()

        viewModel.screenState.test {
            viewModel.startMonitor()
            runCurrent()

            recordingStateFlow.value = RecordingState.Error
            runCurrent()

            expectMostRecentItem().error shouldBe TrafficScreenError.SessionError
        }
    }

    @Test
    fun startMonitor_WhenFlowExceptionThrown_CanBeRestarted() = runTest {
        val viewModel = initViewModel()

        viewModel.startMonitor()
        runCurrent()

        viewModel.screenState.test {
            recordingStateFlow.value = RecordingState.Error
            runCurrent()

            expectMostRecentItem().error shouldBe TrafficScreenError.SessionError

            viewModel.stopMonitor()
            runCurrent()

            viewModel.startMonitor()
            runCurrent()

            val expectedTrafficSession = TrafficSession.fake(number = 456, metricsCount = 2)
            recordingStateFlow.value = RecordingState.Active(expectedTrafficSession)
            runCurrent()

            expectMostRecentItem().trafficDisplayInfo?.session shouldBe expectedTrafficSession
        }
    }

    @Test
    fun consumeErrorState_ClearsErrorState() = runTest {
        val viewModel = initViewModel()

        viewModel.startMonitor()
        runCurrent()

        recordingStateFlow.value = RecordingState.Error
        runCurrent()

        viewModel.screenState.value.error shouldBe TrafficScreenError.SessionError

        viewModel.consumeErrorState()
        runCurrent()

        viewModel.screenState.value.error should beNull()
    }

    private fun TestScope.initViewModel(
        sessionId: Long? = null,
    ): TrafficViewModel {
        val viewModel = TrafficViewModel(
            trafficRepository = trafficRepository,
            sessionId = sessionId,
        )

        viewModel.screenState.launchIn(backgroundScope)

        return viewModel
    }

}
