package com.zackjp.devicedx.feature.dashboard

import android.net.wifi.ScanResult
import com.zackjp.devicedx.data.NetworkRepository
import com.zackjp.devicedx.permissions.AppPermission
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class) // Dispatchers.setMain()/resetMain(), advanceUntilIdle()
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val networkRepository = mockk<NetworkRepository>()
    private val appPermission = mockk<AppPermission>()

    private lateinit var viewModel: DashboardViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { appPermission.hasFineLocation() } returns false
        viewModel = DashboardViewModel(
            networkRepository = networkRepository,
            appPermission = appPermission
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onGetWifiClicked_WithFineLocationAccess_TransformsScanResultsToWifiNames() = runTest(testDispatcher) {
        viewModel.screenState.launchIn(backgroundScope)
        val result1 = ScanResult().apply { SSID = "ssid-name-1" }
        val result2 = ScanResult().apply { SSID = "ssid-name-2" }
        val scanResults = listOf(result1, result2)
        every { networkRepository.getWifiScanFlow() } returns flowOf(scanResults)
        every { appPermission.hasFineLocation() } returns true

        viewModel.onGetWifiClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.screenState.value.wifiNames shouldBe listOf("ssid-name-1", "ssid-name-2")
    }

    @Test
    fun onGetWifiClicked_WithFineLocationAccess_DoesNotEmitRequestPermissionEvent() = runTest {
        val actualEvents = mutableListOf<DashboardEvent>()
        backgroundScope.launch { viewModel.events.collect { actualEvents.add(it) } }
        viewModel.onGetWifiClicked()
        advanceUntilIdle()

        actualEvents shouldBe emptyList()
    }

    @Test
    fun onGetWifiClicked_WithoutFineLocationAccess_EmitsRequestPermissionEvent() = runTest {
        val actualEvents = mutableListOf<DashboardEvent>()
        backgroundScope.launch { viewModel.events.collect { actualEvents.add(it) } }
        viewModel.onGetWifiClicked()
        advanceUntilIdle()

        actualEvents shouldContainExactly listOf(DashboardEvent.LaunchFineLocation)
    }

}