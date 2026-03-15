package com.zackjp.devicedx.data

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [
        TrafficSessionEntity::class,
        TrafficMetricEntity::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trafficDao(): TrafficDao
}
