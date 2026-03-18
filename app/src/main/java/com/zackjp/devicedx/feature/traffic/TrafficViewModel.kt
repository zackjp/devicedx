package com.zackjp.devicedx.feature.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.data.TrafficRepository
import com.zackjp.devicedx.model.TrafficSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TrafficViewModel @Inject constructor(
    private val clock: Clock,
    dispatcherProvider: DispatcherProvider,
    private val trafficRepository: TrafficRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow(
        TrafficScreenState(
            isMonitorActive = false,
            sessionStartTime = null,
            trafficSession = null,
        )
    )
    val screenState = _screenState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _screenState.value)

    private val uiActiveFlow = _screenState.subscriptionCount.map { it > 0 }.distinctUntilChanged()
    private val isMonitorActive = MutableStateFlow(false)

    private val activatableTrafficMonitor: Job = uiActivatedFlow(
        dataSourceProvider = { trafficRepository.recordTrafficMetrics() },
    )
        .onEach(::handleTrafficMetrics)
        .flowOn(dispatcherProvider.default)
        .launchIn(viewModelScope)


    fun startMonitor() {
        _screenState.update {
            it.copy(
                isMonitorActive = true,
                sessionStartTime = clock.now().toEpochMilliseconds(),
            )
        }
        isMonitorActive.value = true
    }

    fun stopMonitor() {
        _screenState.update { it.copy(isMonitorActive = false) }
        isMonitorActive.value = false
    }

    private fun handleTrafficMetrics(trafficSession: TrafficSession) {
        val now = clock.now()
        val timeStart =
            (now - TRAFFIC_METRICS_WINDOW_SECS.seconds).toEpochMilliseconds() / 1000 * 1000

        val filteredSession = trafficSession.copy(
            trafficMetrics = trafficSession.trafficMetrics.takeWhile {
                it.timestamp >= timeStart
            }.reversed()
        )

        _screenState.update { it.copy(trafficSession = filteredSession) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> uiActivatedFlow(
        dataSourceProvider: () -> Flow<T>,
    ): Flow<T> = combine(
        uiActiveFlow,
        isMonitorActive,
    ) { uiActive, isMonitorActive ->
        uiActive && isMonitorActive
    }.distinctUntilChanged(
    ).flatMapLatest { isMonitorActive ->
        if (isMonitorActive) dataSourceProvider() else emptyFlow()
    }

    companion object {
        const val TRAFFIC_METRICS_WINDOW_SECS = 30
    }
}
