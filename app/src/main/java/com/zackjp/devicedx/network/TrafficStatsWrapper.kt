package com.zackjp.devicedx.network

import android.net.TrafficStats
import javax.inject.Inject


class TrafficStatsWrapper @Inject constructor() {

    fun getTotalRxBytes(): Long = TrafficStats.getTotalRxBytes()
    fun getTotalTxBytes(): Long = TrafficStats.getTotalTxBytes()

}
