package com.zackjp.devicedx.feature.traffic

import androidx.compose.runtime.Immutable
import com.zackjp.devicedx.feature.traffic.model.TrafficDisplayInfo

@Immutable
data class TrafficHistoryScreenState(
    val sessions: List<TrafficDisplayInfo>,
)
