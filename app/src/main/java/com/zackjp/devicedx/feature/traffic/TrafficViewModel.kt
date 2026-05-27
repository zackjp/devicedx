package com.zackjp.devicedx.feature.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.data.RecordingState
import com.zackjp.devicedx.data.TrafficRepository
import com.zackjp.devicedx.feature.traffic.model.computeDisplayInfo
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.model.TrafficSession
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = TrafficViewModel.Factory::class)
class TrafficViewModel @AssistedInject constructor(
    private val trafficRepository: TrafficRepository,
    @Assisted private val sessionId: Long?,
) : ViewModel() {

    val screenState = trafficRepository.currentRecordingSession
        .flatMapLatest { recordingState ->
            val recordingSession = (recordingState as? RecordingState.Active)?.session
            val recordingSessionId = recordingSession?.id
            val error =
                (recordingState as? RecordingState.Error)?.let { TrafficScreenError.SessionError }

            val loadedSession = if (sessionId == null || recordingSessionId == sessionId)
                flowOf(recordingSession)
            else
                trafficRepository.getSessionById(sessionId)

            loadedSession.map {
                TrafficScreenState(
                    error = error,
                    graphData = it?.computeFilteredGraphData() ?: emptyList(),
                    recordingSessionId = recordingSessionId,
                    trafficDisplayInfo = it?.computeDisplayInfo(),
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TrafficScreenState(graphData = emptyList(), trafficDisplayInfo = null),
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
        val endTime = endTime ?: trafficMetrics.firstOrNull()?.timestamp ?: 0L
        val endInstant = Instant.fromEpochMilliseconds(endTime)
        val timeStart =
            (endInstant - TRAFFIC_METRICS_WINDOW_SECS.seconds).toEpochMilliseconds() / 1000 * 1000
        return trafficMetrics.takeWhile { it.timestamp >= timeStart }.reversed()
    }

    companion object {
        const val TRAFFIC_METRICS_WINDOW_SECS = 30
    }

    @AssistedFactory
    interface Factory {
        fun create(sessionId: Long?): TrafficViewModel
    }

}
