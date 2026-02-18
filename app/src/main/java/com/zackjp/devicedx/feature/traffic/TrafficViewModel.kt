package com.zackjp.devicedx.feature.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.feature.dashboard.util.TrafficGraphUtil
import com.zackjp.devicedx.model.TrafficData
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
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TrafficViewModel @Inject constructor(
    private val clock: Clock,
    dispatcherProvider: DispatcherProvider,
    realTimeNetworkDataSource: RealTimeNetworkDataSource,
    private val trafficGraphUtil: TrafficGraphUtil,
) : ViewModel() {

    private val _screenState = MutableStateFlow(
        TrafficScreenState(
            isMonitorActive = false,
            trafficMetrics = emptyList(),
        )
    )
    val screenState = _screenState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _screenState.value)

    private val uiActiveFlow = _screenState.subscriptionCount.map { it > 0 }.distinctUntilChanged()
    private val isMonitorActive = MutableStateFlow(false)

    private val activatableTrafficMonitor: Job = uiActivatedFlow(
        dataSourceProvider = realTimeNetworkDataSource::getTrafficStats,
    )
        .runningFold(emptyList(), ::accumulateTrafficHistory)
        .onEach(::handleTrafficStats)
        .flowOn(dispatcherProvider.default)
        .launchIn(viewModelScope)


    fun startMonitor() {
        _screenState.update { it.copy(isMonitorActive = true) }
        isMonitorActive.value = true
    }

    fun stopMonitor() {
        _screenState.update { it.copy(isMonitorActive = false) }
        isMonitorActive.value = false
    }

    private fun accumulateTrafficHistory(
        accumulator: List<TrafficData>,
        trafficData: TrafficData,
    ): List<TrafficData> {
        val startTimeCutoff = clock.now()
            .minus(TRAFFIC_METRICS_WINDOW_SECS.seconds)
            .minus(1.seconds) // accounts for partial data in the starting bucket
            .minus(1.seconds) // accounts for starting metric requiring a prior data point
            .toEpochMilliseconds()
        return accumulator.filter { it.timestamp > startTimeCutoff } + trafficData
    }

    private fun handleTrafficStats(trafficHistory: List<TrafficData>) {
        val now = clock.now()

        val trafficMetrics = trafficGraphUtil.calculateMetrics(
            data = trafficHistory,
            endTime = now.toEpochMilliseconds(),
            TRAFFIC_METRICS_WINDOW_SECS.seconds
        )

        _screenState.update { it.copy(trafficMetrics = trafficMetrics) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> uiActivatedFlow(
        dataSourceProvider: () -> Flow<T>,
    ): Flow<T> = combine(
        uiActiveFlow,
        isMonitorActive,
    ) { uiActive, isMonitorActive ->
        uiActive && isMonitorActive
    }.distinctUntilChanged().flatMapLatest { isMonitorActive ->
        if (isMonitorActive) dataSourceProvider() else emptyFlow()
    }

    companion object {
        const val TRAFFIC_METRICS_WINDOW_SECS = 30
    }
}
