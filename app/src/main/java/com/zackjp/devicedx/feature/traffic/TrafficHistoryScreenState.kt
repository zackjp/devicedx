package com.zackjp.devicedx.feature.traffic

import androidx.compose.runtime.Immutable
import com.zackjp.devicedx.model.TrafficSession

@Immutable
data class TrafficHistoryScreenState(
    val sessions: List<TrafficSession>,
)
