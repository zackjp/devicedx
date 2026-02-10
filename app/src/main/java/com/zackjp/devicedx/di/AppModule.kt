package com.zackjp.devicedx.di

import com.zackjp.devicedx.concurrency.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideDispatcherProvider(): DispatcherProvider = DispatcherProvider()

    @Provides
    @ApplicationScope
    fun provideAppCoroutineScope(dispatcherProvider: DispatcherProvider): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcherProvider.default)

}
