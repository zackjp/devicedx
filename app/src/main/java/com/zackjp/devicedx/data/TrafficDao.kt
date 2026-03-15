package com.zackjp.devicedx.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow


@Dao
interface TrafficDao {
    @Insert
    suspend fun createSession(trafficSessionEntity: TrafficSessionEntity): Long

    @Query("UPDATE traffic_sessions SET endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Long, endTime: Long)

    @Insert
    suspend fun addMetric(trafficMetricEntity: TrafficMetricEntity)

    @Transaction // composite objects perform multiple queries
    @Query("SELECT * FROM traffic_sessions WHERE traffic_sessions.sessionId = :sessionId")
    fun getSessionWithTrafficMetrics(sessionId: Long): Flow<TrafficSessionWithMetrics>
}

@Entity(
    tableName = "traffic_sessions",
)
data class TrafficSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0L,
    val startTime: Long,
    val endTime: Long? = null,
)

@Entity(
    tableName = "traffic_metrics",
    foreignKeys = [
        ForeignKey(
            entity = TrafficSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class TrafficMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val metricId: Long = 0L,
    val sessionId: Long,
    val timestamp: Long,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
)

data class TrafficSessionWithMetrics(
    @Embedded val session: TrafficSessionEntity,
    @Relation(
        parentColumn = "sessionId",
        entityColumn = "sessionId",
    )
    val metrics: List<TrafficMetricEntity>
)