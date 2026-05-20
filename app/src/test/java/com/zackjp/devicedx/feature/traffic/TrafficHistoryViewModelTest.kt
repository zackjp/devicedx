package com.zackjp.devicedx.feature.traffic

import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.TrafficRepository
import com.zackjp.devicedx.model.TrafficSession
import com.zackjp.devicedx.model.fake
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
        private val EXISTING_SESSIONS = listOf(
            TrafficSession.fake(
                number = 1,
                metricsCount = 2,
            ),
            TrafficSession.fake(
                number = 2,
                metricsCount = 3,
            ),
        )
    }

    private val dispatcherProvider = TestDispatcherProvider()

    private val trafficRepository = mockk<TrafficRepository>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcherProvider.default)

        every { trafficRepository.getSessions() } returns flowOf(EXISTING_SESSIONS)
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

        viewModel.state.value.sessions shouldBe EXISTING_SESSIONS
    }

    private fun buildViewModel(): TrafficHistoryViewModel =
        TrafficHistoryViewModel(
            dispatcherProvider = dispatcherProvider,
            trafficRepository = trafficRepository,
        )

}