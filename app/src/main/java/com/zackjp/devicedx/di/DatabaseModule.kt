package com.zackjp.devicedx.di

import android.content.Context
import androidx.room.Room
import com.zackjp.devicedx.data.AppDatabase
import com.zackjp.devicedx.data.TrafficDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            appContext.applicationContext,
            AppDatabase::class.java,
        ).build()
    }

    @Provides
    fun provideTrafficDao(database: AppDatabase): TrafficDao = database.trafficDao()

}
