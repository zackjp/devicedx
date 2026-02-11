package com.zackjp.devicedx.data

import com.zackjp.devicedx.di.ApplicationScope
import com.zackjp.devicedx.network.NetworkUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RealTimeNetworkDataSource @Inject constructor(
    @ApplicationScope appScope: CoroutineScope,
    networkUtility: NetworkUtility,
) {

    private val latencyMillisFlow = flow {
        repeat(20) {
            emit(networkUtility.calculateLatency())
            delay(2000)
        }
    }
        .shareIn(appScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    fun getLatencyMillisFlow(): Flow<Long> = latencyMillisFlow

}
