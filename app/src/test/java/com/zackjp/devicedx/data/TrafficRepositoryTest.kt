package com.zackjp.devicedx.data

import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.feature.traffic.util.TrafficGraphUtil
import com.zackjp.devicedx.flow.FlowCommand
import com.zackjp.devicedx.flow.unwrap
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession
import com.zackjp.devicedx.model.fake
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant


private const val CLOCK_TIME = 1234L
private const val SESSION_ID = 333L


@OptIn(ExperimentalCoroutinesApi::class) // Dispatchers.setMain()/resetMain()
class TrafficRepositoryTest {

    private val clock = mockk<Clock>()
    private val testDispatcherProvider = TestDispatcherProvider()
    private val realTimeNetworkDataSource = mockk<RealTimeNetworkDataSource>()
    private val trafficDao = mockk<TrafficDao>()
    private val trafficGraphUtil = mockk<TrafficGraphUtil>()

    private val trafficSessionReadFlow = MutableSharedFlow<FlowCommand<TrafficSessionWithMetrics>>()
    private val trafficSessionWriteFlow = MutableSharedFlow<FlowCommand<TrafficMetric>>()
    private val trafficSessionsFlow = MutableSharedFlow<FlowCommand<List<TrafficSessionEntity>>>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcherProvider.default)

        every { clock.now() } returns Instant.fromEpochMilliseconds(CLOCK_TIME)
        every { realTimeNetworkDataSource.getTrafficStats() } returns MutableSharedFlow()
        coEvery { trafficDao.addMetricAndSync(any()) } just runs
        coEvery { trafficDao.createSession(any()) } returns SESSION_ID
        coEvery { trafficDao.updateSessionEndTime(any(), any()) } just runs
        coEvery { trafficDao.getSessionWithTrafficMetrics(SESSION_ID) } returns trafficSessionReadFlow.unwrap()
        every { trafficDao.getSessions() } returns trafficSessionsFlow.unwrap()
        every { trafficGraphUtil.runningMetricsCalculation(any()) } returns trafficSessionWriteFlow.unwrap()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startRecording_InsertsTrafficSessionToDb() = runTest {
        buildRepository().startRecording()
        runCurrent()

        coVerify {
            trafficDao.createSession(
                TrafficSessionEntity(
                    sessionId = 0L,
                    startTime = CLOCK_TIME,
                    endTime = null,
                )
            )
        }
    }

    @Test
    fun startRecording_WritesEachMetricItemToDb() = runTest {
        buildRepository().startRecording()
        runCurrent()

        val metric1 = TrafficMetric.fake(11)
        val metric2 = TrafficMetric.fake(301)

        trafficSessionWriteFlow.emit(FlowCommand.Emit(metric1))
        runCurrent()
        coVerify { trafficDao.addMetricAndSync(metric1.toEntity(SESSION_ID)) }

        trafficSessionWriteFlow.emit(FlowCommand.Emit(metric2))
        runCurrent()
        coVerify { trafficDao.addMetricAndSync(metric2.toEntity(SESSION_ID)) }
    }

    @Test
    fun startRecording_WhenDatabaseReadFlowEmitsSessionEntity_EmitsDomainModel() = runTest {
        val sessionStartTime = 12345L
        val expectedTotalRxBytes = 8888L
        val expectedTotalTxBytes = 9999L

        val metricsEntity = TrafficSessionWithMetrics(
            TrafficSessionEntity(
                SESSION_ID,
                sessionStartTime,
                totalRxBytes = expectedTotalRxBytes,
                totalTxBytes = expectedTotalTxBytes,
            ),
            listOf(
                TrafficMetricEntity(
                    metricId = 10L,
                    sessionId = SESSION_ID,
                    timestamp = 1250L,
                    rxBytesPerSec = 80_000L,
                    txBytesPerSec = 35_000L,
                ),
            ),
        )
        val expectedDomain = TrafficSession(
            id = SESSION_ID,
            startTime = sessionStartTime,
            totalRxBytes = expectedTotalRxBytes,
            totalTxBytes = expectedTotalTxBytes,
            trafficMetrics = listOf(
                TrafficMetric(
                    timestamp = 1250L,
                    rxBytesPerSec = 80_000L,
                    txBytesPerSec = 35_000L,
                )
            ),
        )

        val repository = buildRepository()
        repository.currentActiveSession.test {
            awaitItem() shouldBe RecordingState.Idle

            repository.startRecording()
            runCurrent()

            trafficSessionReadFlow.emit(FlowCommand.Emit(metricsEntity))
            runCurrent()

            awaitItem() shouldBe RecordingState.Active(expectedDomain)
        }
    }

    @Test
    fun stopRecording_UpdatesSessionEndTime() = runTest {
        val repository = buildRepository()
        repository.startRecording()
        runCurrent()

        every { clock.now() } returns Instant.fromEpochMilliseconds(9876543210L)

        repository.stopRecording()
        runCurrent()

        coVerify { trafficDao.updateSessionEndTime(sessionId = SESSION_ID, endTime = 9876543210L) }
    }

    @Test
    fun stopRecording_ResetsRecordingStateToIdle() = runTest {
        val repository = buildRepository()
        repository.startRecording()
        runCurrent()

        trafficSessionReadFlow.emit(
            FlowCommand.Emit(
                TrafficSessionWithMetrics(
                    TrafficSessionEntity(SESSION_ID, CLOCK_TIME), emptyList()
                )
            )
        )
        runCurrent()

        repository.stopRecording()
        runCurrent()

        repository.currentActiveSession.value shouldBe RecordingState.Idle
    }

    @Test
    fun startRecording_WhenFlowThrows_AttemptsSessionEndTimeUpdate() = runTest {
        val repository = buildRepository()
        repository.startRecording()
        runCurrent()

        every { clock.now() } returns Instant.fromEpochMilliseconds(9876543210L)

        trafficSessionWriteFlow.emit(FlowCommand.Throw(CustomException("Fake exception")))
        runCurrent()

        coVerify { trafficDao.updateSessionEndTime(sessionId = SESSION_ID, endTime = 9876543210L) }
    }

    @Test
    fun startRecording_WhenPhase1SessionDbCreationFails_SetsErrorState() = runTest {
        coEvery { trafficDao.createSession(any()) } throws CustomException("Create session fails")

        val repository = buildRepository()
        repository.startRecording()
        runCurrent()

        repository.currentActiveSession.value shouldBe RecordingState.Error
    }

    @Test
    fun startRecording_WhenPhase2ReadDbFlowErrs_SetsErrorState() = runTest {
        val repository = buildRepository()
        repository.startRecording()
        runCurrent()

        trafficSessionReadFlow.emit(FlowCommand.Throw(CustomException("Fake db read exception")))
        runCurrent()

        repository.currentActiveSession.value shouldBe RecordingState.Error
    }

    @Test
    fun startRecording_WhenPhase2WriteDbFlowErrs_SetsErrorState() = runTest {
        val repository = buildRepository()
        repository.startRecording()
        runCurrent()

        trafficSessionWriteFlow.emit(FlowCommand.Throw(CustomException("Fake db write exception")))
        runCurrent()

        repository.currentActiveSession.value shouldBe RecordingState.Error
    }

    @Test
    fun startRecording_WhenCalledTwice_CancelsFirstJob() = runTest {
        val repository = buildRepository()
        repository.startRecording()
        runCurrent()

        every { clock.now() } returns Instant.fromEpochMilliseconds(5678L)

        repository.startRecording()
        runCurrent()

        coVerify(exactly = 2) { trafficDao.createSession(any()) }
        coVerify { trafficDao.updateSessionEndTime(SESSION_ID, 5678L) }
    }

    @Test
    fun getSessions_WhenEmitsEntity_MapsToDomainModel() = runTest {
        val repository = buildRepository()

        val sessionDomainsFlow = repository.getSessions()

        val entityModels = listOf(
            TrafficSessionEntity(
                sessionId = 123L,
                startTime = 456L,
                endTime = 789L,
                totalRxBytes = 111L,
                totalTxBytes = 333L,
            ),
        )
        val domainModels = listOf(
            TrafficSession(
                id = 123L,
                startTime = 456L,
                endTime = 789L,
                totalRxBytes = 111L,
                totalTxBytes = 333L,
                trafficMetrics = emptyList(),
            ),
        )

        sessionDomainsFlow.test {
            trafficSessionsFlow.emit(FlowCommand.Emit(entityModels))

            awaitItem() shouldBe domainModels
        }
    }

    @Test
    fun getSessionById_WhenEmits_MapsToDomainModel() = runTest {
        val metricsEntity = TrafficSessionWithMetrics(
            TrafficSessionEntity(
                SESSION_ID,
                12345L,
                totalRxBytes = 8888L,
                totalTxBytes = 9999L,
            ),
            listOf(
                TrafficMetricEntity(
                    metricId = 10L,
                    sessionId = SESSION_ID,
                    timestamp = 1250L,
                    rxBytesPerSec = 80_000L,
                    txBytesPerSec = 35_000L,
                ),
            ),
        )
        val expectedDomain = TrafficSession(
            id = SESSION_ID,
            startTime = 12345L,
            totalRxBytes = 8888L,
            totalTxBytes = 9999L,
            trafficMetrics = listOf(
                TrafficMetric(
                    timestamp = 1250L,
                    rxBytesPerSec = 80_000L,
                    txBytesPerSec = 35_000L,
                )
            ),
        )

        val repository = buildRepository()

        repository.getSessionById(SESSION_ID).test {
            trafficSessionReadFlow.emit(FlowCommand.Emit(metricsEntity))

            expectMostRecentItem() shouldBe expectedDomain
        }
    }

    private fun TestScope.buildRepository() = TrafficRepository(
        appScope = backgroundScope,
        clock = clock,
        dispatcherProvider = testDispatcherProvider,
        realTimeNetworkDataSource = realTimeNetworkDataSource,
        trafficDao = trafficDao,
        trafficGraphUtil = trafficGraphUtil,
    )

}

private class CustomException(msg: String) : Exception(msg)

private fun TrafficMetric.toEntity(sessionId: Long): TrafficMetricEntity =
    TrafficMetricEntity(
        metricId = 0L,
        sessionId = sessionId,
        timestamp = timestamp,
        rxBytesPerSec = rxBytesPerSec,
        txBytesPerSec = txBytesPerSec,
    )
