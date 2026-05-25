package com.zackjp.devicedx.feature.traffic

import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.TrafficRepository
import com.zackjp.devicedx.feature.traffic.model.TrafficDisplayInfo
import com.zackjp.devicedx.model.Bytes.Companion.asDataUnit
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficSession
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrafficHistoryViewModelTest {

    private companion object {
        private val REPOSITORY_SESSIONS = listOf(
            TrafficSession(
                id = 7L,
                startTime = 1234L,
                totalTxBytes = 111111L,
                totalRxBytes = 333333L,
                trafficMetrics = emptyList(),
            ),
            TrafficSession(
                id = 13L,
                startTime = 3456L,
                totalTxBytes = 555555L,
                totalRxBytes = 777777L,
                trafficMetrics = emptyList(),
            ),
        )
        private val SESSION_ITEMS = listOf(
            TrafficDisplayInfo(
                session = REPOSITORY_SESSIONS[0],
                totalTxValue = REPOSITORY_SESSIONS[0].totalTxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit.first,
                totalTxUnit = REPOSITORY_SESSIONS[0].totalTxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit.second,
                totalRxValue = REPOSITORY_SESSIONS[0].totalRxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit.first,
                totalRxUnit = REPOSITORY_SESSIONS[0].totalRxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit.second,
            ),
            TrafficDisplayInfo(
                session = REPOSITORY_SESSIONS[1],
                totalTxValue = REPOSITORY_SESSIONS[1].totalTxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit.first,
                totalTxUnit = REPOSITORY_SESSIONS[1].totalTxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit.second,
                totalRxValue = REPOSITORY_SESSIONS[1].totalRxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit.first,
                totalRxUnit = REPOSITORY_SESSIONS[1].totalRxBytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit.second,
            ),
        )
    }

    private val dispatcherProvider = TestDispatcherProvider()

    private val trafficRepository = mockk<TrafficRepository>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcherProvider.default)

        every { trafficRepository.getSessions() } returns flowOf(REPOSITORY_SESSIONS)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stateSubscription_WhenFirstSubscribed_LoadsSessionsFromRepo() = runTest {
        val viewModel = buildViewModel()
        runCurrent()
        viewModel.state.value.sessions shouldBe emptyList()

        viewModel.state.launchIn(backgroundScope)
        runCurrent()

        viewModel.state.value.sessions shouldBe SESSION_ITEMS
    }

    private fun buildViewModel(): TrafficHistoryViewModel =
        TrafficHistoryViewModel(
            dispatcherProvider = dispatcherProvider,
            trafficRepository = trafficRepository,
        )

}