package com.zackjp.devicedx.feature.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zackjp.devicedx.concurrency.DispatcherProvider
import com.zackjp.devicedx.data.TrafficRepository
import com.zackjp.devicedx.model.Bytes.Companion.asDataUnit
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrafficHistoryViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    trafficRepository: TrafficRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TrafficHistoryScreenState(
            sessions = emptyList(),
        )
    )

    val state = _state.asStateFlow()
        .combine(trafficRepository.getSessions()) { state, sessions ->
            state.copy(sessions = sessions.map { it.toSessionInfo() })
        }
        .flowOn(dispatcherProvider.default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), _state.value)

    private fun TrafficSession.toSessionInfo(): TrafficSessionInfo {
        val rxCalculationResult = this.totalRxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit
        val txCalculationResult = this.totalTxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit

        return TrafficSessionInfo(
            sessionId = this.id,
            rxValue = rxCalculationResult.first.toFloat(),
            rxUnit = rxCalculationResult.second,
            txValue = txCalculationResult.first.toFloat(),
            txUnit = txCalculationResult.second,
        )
    }

}