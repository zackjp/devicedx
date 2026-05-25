package com.zackjp.devicedx.feature.traffic.model

import androidx.compose.runtime.Immutable
import com.zackjp.devicedx.model.Bytes.Companion.asDataUnit
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficSession
import java.math.BigDecimal


@Immutable
data class TrafficDisplayInfo(
    val session: TrafficSession,
    val totalRxValue: BigDecimal,
    val totalRxUnit: DataUnit,
    val totalTxValue: BigDecimal,
    val totalTxUnit: DataUnit,
)


fun TrafficSession.computeDisplayInfo(): TrafficDisplayInfo {
    val rxCalculationResult = this.totalRxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit
    val txCalculationResult = this.totalTxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit

    return TrafficDisplayInfo(
        session = this,
        totalRxValue = rxCalculationResult.first,
        totalRxUnit = rxCalculationResult.second,
        totalTxValue = txCalculationResult.first,
        totalTxUnit = txCalculationResult.second,
    )
}
