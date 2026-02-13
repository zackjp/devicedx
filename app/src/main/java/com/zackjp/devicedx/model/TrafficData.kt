package com.zackjp.devicedx.model

data class TrafficData(
    val timestamp: Long,
    val txBytes: Long,
) { companion object }
