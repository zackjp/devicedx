package com.zackjp.devicedx.data

import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.feature.traffic.util.TrafficGraphUtil
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
class TrafficRepository @Inject constructor(
    private val clock: Clock,
    private val dispatcherProvider: DispatcherProvider,
    private val realTimeNetworkDataSource: RealTimeNetworkDataSource,
    private val trafficDao: TrafficDao,
    private val trafficGraphUtil: TrafficGraphUtil,
) {

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

    fun recordTrafficMetrics(): Flow<TrafficSession> = recordAndObserveTrafficMetricsSession

    private fun recordMetricsToDbFlow(sessionId: Long) =
        trafficGraphUtil.runningMetricsCalculation(realTimeNetworkDataSource.getTrafficStats())
            .onEach { trafficMetric ->
                trafficDao.addMetric(
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
        startTime = this.session.startTime,
        endTime = this.session.endTime,
        trafficMetrics = this.metrics.map {
            TrafficMetric(
                timestamp = it.timestamp,
                rxBytesPerSec = it.rxBytesPerSec,
                txBytesPerSec = it.txBytesPerSec,
            )
        }
    )
