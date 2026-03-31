package com.zackjp.devicedx.data

import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.feature.traffic.util.TrafficGraphUtil
import com.zackjp.devicedx.flow.FlowCommand
import com.zackjp.devicedx.flow.unwrap
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession
import com.zackjp.devicedx.model.fake
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
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

    lateinit var trafficRepository: TrafficRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcherProvider.default)

        every { clock.now() } returns Instant.fromEpochMilliseconds(CLOCK_TIME)
        every { realTimeNetworkDataSource.getTrafficStats() } returns MutableSharedFlow()
        coEvery { trafficDao.addMetricAndSync(any()) } just runs
        coEvery { trafficDao.createSession(any()) } returns SESSION_ID
        coEvery { trafficDao.updateSessionEndTime(any(), any()) } just runs
        coEvery { trafficDao.getSessionWithTrafficMetrics(SESSION_ID) } returns trafficSessionReadFlow.unwrap()
        every { trafficGraphUtil.runningMetricsCalculation(any()) } returns trafficSessionWriteFlow.unwrap()

        trafficRepository = TrafficRepository(
            clock = clock,
            dispatcherProvider = testDispatcherProvider,
            realTimeNetworkDataSource = realTimeNetworkDataSource,
            trafficDao = trafficDao,
            trafficGraphUtil = trafficGraphUtil
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun recordTrafficMetrics_InsertsTrafficSessionToDb() = runTest {
        trafficRepository.recordTrafficMetrics().launchIn(backgroundScope)
        runCurrent() // initialize the session

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
    fun recordTrafficMetrics_WritesEachMetricItemToDb() = runTest {
        trafficRepository.recordTrafficMetrics().launchIn(backgroundScope)
        runCurrent() // initialize the session

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
    fun recordTrafficMetrics_EmitsTrafficMetricItemsFromDb() = runTest {
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

        trafficRepository.recordTrafficMetrics().test {
            runCurrent() // initiate the flow with session creation and db observable

            trafficSessionReadFlow.emit(FlowCommand.Emit(metricsEntity))
            runCurrent()

            awaitItem() shouldBe expectedDomain
        }
    }

    @Test
    fun recordTrafficMetrics_WhenFlowCompletesNormally_UpdatesSessionEndTime() = runTest {
        val job = trafficRepository.recordTrafficMetrics().launchIn(backgroundScope)
        runCurrent() // initialize the session

        every { clock.now() } returns Instant.fromEpochMilliseconds(9876543210L)

        job.cancel()
        runCurrent()

        coVerify { trafficDao.updateSessionEndTime(sessionId = SESSION_ID, endTime = 9876543210L) }
    }

    @Test
    fun recordTrafficMetrics_WhenFlowCompletesExceptionally_AttemptsSessionEndUpdate() = runTest {
        trafficRepository.recordTrafficMetrics()
            .catch { /* don't crash the test */ }
            .launchIn(backgroundScope)
        runCurrent()

        every { clock.now() } returns Instant.fromEpochMilliseconds(9876543210L)

        trafficSessionWriteFlow.emit(FlowCommand.Throw(CustomException("Fake db write exception")))
        runCurrent()

        coVerify { trafficDao.updateSessionEndTime(sessionId = SESSION_ID, endTime = 9876543210L) }
    }

    @Test
    fun recordTrafficMetrics_WhenPhase1SessionDbCreationFails_PropagatesException() = runTest {
        coEvery { trafficDao.createSession(any()) } throws CustomException("Create session fails")

        shouldThrow<CustomException> {
            trafficRepository.recordTrafficMetrics().collect { }
            runCurrent()
        }
    }

    @Test
    fun recordTrafficMetrics_WhenPhase2ReadDbFlowErrs_PropagatesException() = runTest {
        var capturedException: Throwable? = null

        trafficRepository.recordTrafficMetrics()
            .catch { capturedException = it }
            .launchIn(backgroundScope)
        runCurrent()

        trafficSessionReadFlow.emit(FlowCommand.Throw(CustomException("Fake db read exception")))
        runCurrent()

        capturedException.shouldBeInstanceOf<CustomException>()
    }

    @Test
    fun recordTrafficMetrics_WhenPhase2WriteDbFlowErrs_PropagatesException() = runTest {
        var capturedException: Throwable? = null

        trafficRepository.recordTrafficMetrics()
            .catch { capturedException = it }
            .launchIn(backgroundScope)
        runCurrent()

        trafficSessionWriteFlow.emit(FlowCommand.Throw(CustomException("Fake db write exception")))
        runCurrent()

        capturedException.shouldBeInstanceOf<CustomException>()
    }

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
