package com.zackjp.devicedx.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher


class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher = StandardTestDispatcher(),
) : DispatcherProvider(
    default = dispatcher,
    io = dispatcher,
    main = dispatcher
)
