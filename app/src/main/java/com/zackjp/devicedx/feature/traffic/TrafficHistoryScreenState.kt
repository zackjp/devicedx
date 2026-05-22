package com.zackjp.devicedx.feature.traffic

import androidx.compose.runtime.Immutable
import com.zackjp.devicedx.model.DataUnit

@Immutable
data class TrafficHistoryScreenState(
    val sessions: List<TrafficSessionInfo>,
)

@Immutable
data class TrafficSessionInfo(
    val sessionId: Long,
    val rxValue: Float,
    val rxUnit: DataUnit,
    val txValue: Float,
    val txUnit: DataUnit,
)
