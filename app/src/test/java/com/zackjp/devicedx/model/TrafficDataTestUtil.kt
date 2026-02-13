package com.zackjp.devicedx.model


fun TrafficData.Companion.fake(number: Long): TrafficData = TrafficData(
    timestamp = number * 1000L,
    txBytes = number,
)