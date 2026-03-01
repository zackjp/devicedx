package com.zackjp.devicedx.data

import com.zackjp.devicedx.di.ApplicationScope
import com.zackjp.devicedx.model.TrafficData
import com.zackjp.devicedx.network.NetworkUtility
import com.zackjp.devicedx.network.TrafficStatsWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock


@Singleton
class RealTimeNetworkDataSource @Inject constructor(
    networkUtility: NetworkUtility,
    private val clock: Clock,
    @ApplicationScope appScope: CoroutineScope,
    trafficStatsWrapper: TrafficStatsWrapper,
) {

    private val latencyMillisFlow = flow {
        while (true) {
            emit(networkUtility.calculateLatency())
            delay(2000)
        }
    }.shareIn(appScope, SharingStarted.WhileSubscribed(5000), replay = 0)

    private val trafficStats = flow {
        while (true) {
            val dataPoint = TrafficData(
                timestamp = clock.now().toEpochMilliseconds(),
                rxBytes = trafficStatsWrapper.getTotalRxBytes(),
                txBytes = trafficStatsWrapper.getTotalTxBytes(),
            )
            emit(dataPoint)
            delay(1000)
        }
    }.shareIn(appScope, SharingStarted.WhileSubscribed(1000), 0)

    fun getLatencyMillisFlow(): Flow<Long> = latencyMillisFlow

    fun getTrafficStats(): Flow<TrafficData> = trafficStats

}
