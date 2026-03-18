package com.zackjp.devicedx.data

import androidx.room.Dao
import androidx.room.DatabaseView
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
abstract class TrafficDao {

    @Insert
    protected abstract suspend fun insertMetric(trafficMetricEntity: TrafficMetricEntity)

    @Query("""
        UPDATE traffic_sessions
        SET totalRxBytes = totalRxBytes + :rxBytes, 
            totalTxBytes = totalTxBytes + :txBytes
        WHERE sessionId = :sessionId
    """)
    protected abstract suspend fun updateTrafficSessionTotals(
        sessionId: Long,
        rxBytes: Long,
        txBytes: Long,
    )

    @Insert
    abstract suspend fun createSession(trafficSessionEntity: TrafficSessionEntity): Long

    @Query("UPDATE traffic_sessions SET endTime = :endTime WHERE sessionId = :sessionId")
    abstract suspend fun updateSessionEndTime(sessionId: Long, endTime: Long)

    @Transaction
    open suspend fun addMetricAndSync(trafficMetricEntity: TrafficMetricEntity) {
        insertMetric(trafficMetricEntity)
        updateTrafficSessionTotals(
            sessionId = trafficMetricEntity.sessionId,
            rxBytes = trafficMetricEntity.rxBytesPerSec,
            txBytes = trafficMetricEntity.txBytesPerSec,
        )
    }

    @Transaction // composite objects perform multiple queries
    @Query("SELECT * FROM traffic_sessions WHERE traffic_sessions.sessionId = :sessionId")
    abstract fun getSessionWithTrafficMetrics(sessionId: Long): Flow<TrafficSessionWithMetrics>
}

@Entity(
    tableName = "traffic_sessions",
)
data class TrafficSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0L,
    val startTime: Long,
    val endTime: Long? = null,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
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
        entity = TrafficMetricSortedDescView::class,
        parentColumn = "sessionId",
        entityColumn = "sessionId",
    )
    val metrics: List<TrafficMetricEntity>
)

@DatabaseView("SELECT * FROM traffic_metrics ORDER BY timestamp DESC")
data class TrafficMetricSortedDescView(
    val metricId: Long,
    val sessionId: Long,
    val timestamp: Long,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long
)
