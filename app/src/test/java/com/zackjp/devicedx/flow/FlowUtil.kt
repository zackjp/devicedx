package com.zackjp.devicedx.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


sealed interface FlowCommand<T> {
    data class Emit<T>(val value: T) : FlowCommand<T>
    data class Throw<T>(val throwable: Throwable) : FlowCommand<T>
}

fun <T> Flow<FlowCommand<T>>.unwrap(): Flow<T> = map {
    when (it) {
        is FlowCommand.Emit -> it.value
        is FlowCommand.Throw -> throw it.throwable
    }
}
