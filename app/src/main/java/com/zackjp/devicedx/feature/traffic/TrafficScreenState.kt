package com.zackjp.devicedx.feature.traffic

import androidx.compose.runtime.Immutable
import com.zackjp.devicedx.feature.traffic.model.TrafficDisplayInfo
import com.zackjp.devicedx.model.TrafficMetric


@Immutable
data class TrafficScreenState(
    val error: TrafficScreenError? = null,
    val graphData: List<TrafficMetric>,
    val recordingSessionId: Long? = null,
    val trafficDisplayInfo: TrafficDisplayInfo?,
)

sealed interface TrafficScreenError {
    data object SessionError : TrafficScreenError
}
