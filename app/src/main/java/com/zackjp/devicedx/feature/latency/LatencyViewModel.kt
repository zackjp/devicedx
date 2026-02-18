package com.zackjp.devicedx.feature.latency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
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

@HiltViewModel
class LatencyViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    realTimeNetworkDataSource: RealTimeNetworkDataSource,
) : ViewModel() {

    private val _screenState = MutableStateFlow(
        LatencyScreenState(
            isMonitorActive = false,
            latencyHistory = emptyList(),
        )
    )
    val screenState = _screenState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _screenState.value)


    private val uiActiveFlow = _screenState.subscriptionCount.map { it > 0 }.distinctUntilChanged()
    private val isMonitorActive = MutableStateFlow(false)

    private val activatableLatencyMonitor: Job = uiActivatedFlow(
        dataSourceProvider = realTimeNetworkDataSource::getLatencyMillisFlow,
    )
        .onEach(::handleNewLatencyMetric)
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


    private fun handleNewLatencyMetric(latencyMillis: Long) {
        _screenState.update {
            it.copy(
                latencyHistory =
                    (it.latencyHistory + latencyMillis).takeLast(MAX_LATENCY_DATA_POINTS)
            )
        }
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
        const val MAX_LATENCY_DATA_POINTS = 10
    }

}
