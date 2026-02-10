package com.zackjp.devicedx.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


open class DispatcherProvider @Inject constructor(
    val default: CoroutineDispatcher = Dispatchers.Default,
    val io: CoroutineDispatcher = Dispatchers.IO,
    val main: CoroutineDispatcher = Dispatchers.Main,
)
