package com.zackjp.devicedx.model


fun TrafficData.Companion.fake(number: Long): TrafficData = TrafficData(
    timestamp = number * 1000L,
    rxBytes = number * 100 + 1,
    txBytes = number * 100 + 2,
)