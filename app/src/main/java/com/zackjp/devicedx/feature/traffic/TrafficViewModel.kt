package com.zackjp.devicedx.feature.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.data.RecordingState
import com.zackjp.devicedx.data.TrafficRepository
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TrafficViewModel @Inject constructor(
    private val clock: Clock,
    private val trafficRepository: TrafficRepository,
) : ViewModel() {

    val screenState = trafficRepository.recordingState
        .map { recording ->
            val session = (recording as? RecordingState.Active)?.session
            val error = if (recording as? RecordingState.Error != null) TrafficScreenError.SessionError else null
            TrafficScreenState(
                trafficSession = session,
                graphData = session?.computeFilteredGraphData() ?: emptyList(),
                error = error,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TrafficScreenState(graphData = emptyList(), trafficSession = null),
        )

    fun startMonitor() {
        trafficRepository.startRecording()
    }

    fun stopMonitor() {
        trafficRepository.stopRecording()
    }

    fun consumeErrorState() {
        trafficRepository.stopRecording()
    }

    private fun TrafficSession.computeFilteredGraphData(): List<TrafficMetric> {
        val timeStart =
            (clock.now() - TRAFFIC_METRICS_WINDOW_SECS.seconds).toEpochMilliseconds() / 1000 * 1000
        return trafficMetrics.takeWhile { it.timestamp >= timeStart }.reversed()
    }

    companion object {
        const val TRAFFIC_METRICS_WINDOW_SECS = 30
    }
}
