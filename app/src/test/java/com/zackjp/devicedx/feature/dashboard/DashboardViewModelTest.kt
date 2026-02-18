package com.zackjp.devicedx.feature.dashboard

import android.net.wifi.ScanResult
import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.system.permissions.PermissionChecker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class) // Dispatchers.setMain()/resetMain(), advanceUntilIdle()
class DashboardViewModelTest {

    private val testDispatcherProvider = TestDispatcherProvider()

    private val wifiDataSource = mockk<WifiDataSource>()
    private val permissionChecker = mockk<PermissionChecker>()

    private lateinit var viewModel: DashboardViewModel


    private companion object {
        val result1 = ScanResult().apply { SSID = "ssid-name-1" }
        val result2 = ScanResult().apply { SSID = "ssid-name-2" }
        val scanResults = listOf(result1, result2)
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcherProvider.default)

        every { permissionChecker.hasFineLocation() } returns false
        every { wifiDataSource.getWifiScanFlow() } returns flowOf(scanResults)
        viewModel = DashboardViewModel(
            dispatcherProvider = testDispatcherProvider,
            permissionChecker = permissionChecker,
            wifiDataSource = wifiDataSource,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onStartScan_UpdatesActiveViewToWifi() = runTest {
        initViewModel()

        viewModel.screenState.test {
            awaitItem().activeView shouldNotBe DashboardView.Wifi

            viewModel.onStartScan()
            advanceUntilIdle()

            expectMostRecentItem().activeView shouldBe DashboardView.Wifi
        }
    }

    @Test
    fun onStartScan_WithFineLocationAccess_TransformsScanResultsToWifiNames() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.onStartScan()
        advanceUntilIdle()

        val state = viewModel.screenState.value
        state.wifiNames shouldBe listOf("ssid-name-1", "ssid-name-2")
    }

    @Test
    fun onStartScan_WithFineLocationAccess_SetsPermissionStatusToGranted() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.onStartScan()
        advanceUntilIdle()

        val state = viewModel.screenState.value
        state.permissionStatus shouldBe PermissionStatus.Granted
    }

    @Test
    fun onStartScan_WithFineLocationAccess_DoesNotEmitRequestPermissionEvent() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.events.test {
            viewModel.onStartScan()
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun onStartScan_WithoutFineLocationAccess_EmitsRequestPermissionEventOnlyOnce() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns false

        viewModel.events.test {
            viewModel.onStartScan()
            advanceUntilIdle()
            awaitItem() shouldBe DashboardEvent.LaunchFineLocation

            viewModel.onStartScan()
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun onStartScan_WithoutFineLocationAccess_SetsPermissionStatusToPending() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns false

        viewModel.onStartScan()
        advanceUntilIdle()

        val state = viewModel.screenState.value
        state.permissionStatus shouldBe PermissionStatus.Pending
    }

    @Test
    fun onFineLocationPermissionDenied_SetsPermissionStatusToDenied() = runTest {
        initViewModel()

        viewModel.screenState.test {
            viewModel.onFineLocationPermissionDenied()
            advanceUntilIdle()

            expectMostRecentItem().permissionStatus shouldBe PermissionStatus.Denied
        }
    }

    private fun TestScope.initViewModel() {
        viewModel.screenState.launchIn(backgroundScope)
    }

}
