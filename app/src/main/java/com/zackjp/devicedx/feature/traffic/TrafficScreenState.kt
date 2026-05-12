package com.zackjp.devicedx.feature.traffic

import androidx.compose.runtime.Immutable
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession


@Immutable
data class TrafficScreenState(
    val error: TrafficScreenError? = null,
    val graphData: List<TrafficMetric>,
    val trafficSession: TrafficSession?,
)

sealed interface TrafficScreenError {
    data object SessionError : TrafficScreenError
}
