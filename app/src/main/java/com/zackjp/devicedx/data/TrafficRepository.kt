package com.zackjp.devicedx.data

import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.di.ApplicationScope
import com.zackjp.devicedx.feature.traffic.util.TrafficGraphUtil
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Active(val session: TrafficSession) : RecordingState
    data object Error : RecordingState
}

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
@Singleton
class TrafficRepository @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    private val clock: Clock,
    private val dispatcherProvider: DispatcherProvider,
    private val realTimeNetworkDataSource: RealTimeNetworkDataSource,
    private val trafficDao: TrafficDao,
    private val trafficGraphUtil: TrafficGraphUtil,
) {

    private val _currentActiveSession = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val currentActiveSession: StateFlow<RecordingState> = _currentActiveSession.asStateFlow()

    private var recordingJob: Job? = null

    private val recordAndObserveTrafficMetricsSession: Flow<TrafficSession> =
        flow {
            val startTime = clock.now().toEpochMilliseconds()
            val sessionId = trafficDao.createSession(TrafficSessionEntity(startTime = startTime))
            emit(sessionId)
        }.flatMapLatest { sessionId ->
            val readMetricsFromDbFlow =
                trafficDao.getSessionWithTrafficMetrics(sessionId = sessionId)
            val writeMetricsToDbFlow = recordMetricsToDbFlow(sessionId)

            merge(
                readMetricsFromDbFlow,
                writeMetricsToDbFlow.ignoreElements(),
            )
                .onCompletion {
                    withContext(NonCancellable + dispatcherProvider.io) {
                        trafficDao.updateSessionEndTime(
                            sessionId,
                            clock.now().toEpochMilliseconds(),
                        )
                    }
                }
        }.map { sessionWithMetrics ->
            sessionWithMetrics.toDomain()
        }.flowOn(dispatcherProvider.io)

    fun getSessionById(sessionId: Long): Flow<TrafficSession> =
        trafficDao.getSessionWithTrafficMetrics(sessionId)
            .map { it.toDomain() }

    fun startRecording() {
        recordingJob?.cancel()
        recordingJob = recordAndObserveTrafficMetricsSession
            .onStart { _currentActiveSession.value = RecordingState.Idle }
            .onEach { session -> _currentActiveSession.value = RecordingState.Active(session) }
            .onCompletion { cause ->
                _currentActiveSession.value = when {
                    cause == null || cause is CancellationException -> RecordingState.Idle
                    else -> RecordingState.Error
                }
            }
            .catch { }
            .launchIn(appScope)
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
    }

    fun getSessions(): Flow<List<TrafficSession>> =
        trafficDao.getSessions().map { sessionEntitiesList ->
            sessionEntitiesList.map { entity ->
                entity.toDomain()
            }
        }

    private fun recordMetricsToDbFlow(sessionId: Long) =
        trafficGraphUtil.runningMetricsCalculation(realTimeNetworkDataSource.getTrafficStats())
            .onEach { trafficMetric ->
                trafficDao.addMetricAndSync(
                    TrafficMetricEntity(
                        sessionId = sessionId,
                        timestamp = trafficMetric.timestamp,
                        rxBytesPerSec = trafficMetric.rxBytesPerSec,
                        txBytesPerSec = trafficMetric.txBytesPerSec,
                    )
                )
            }

}

private fun Flow<*>.ignoreElements() = filter { false }.mapNotNull { null }

private fun TrafficSessionWithMetrics.toDomain(): TrafficSession =
    TrafficSession(
        id = this.session.sessionId,
        startTime = this.session.startTime,
        endTime = this.session.endTime,
        totalRxBytes = this.session.totalRxBytes,
        totalTxBytes = this.session.totalTxBytes,
        trafficMetrics = this.metrics.map {
            TrafficMetric(
                timestamp = it.timestamp,
                rxBytesPerSec = it.rxBytesPerSec,
                txBytesPerSec = it.txBytesPerSec,
            )
        }
    )

private fun TrafficSessionEntity.toDomain(): TrafficSession =
    TrafficSession(
        id = this.sessionId,
        startTime = this.startTime,
        endTime = this.endTime,
        totalRxBytes = this.totalRxBytes,
        totalTxBytes = this.totalTxBytes,
        trafficMetrics = emptyList(),
    )
