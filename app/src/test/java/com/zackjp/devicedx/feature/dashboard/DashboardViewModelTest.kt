package com.zackjp.devicedx.feature.dashboard

import android.net.wifi.ScanResult
import app.cash.turbine.test
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.feature.dashboard.DashboardViewModel.Companion.MAX_LATENCY_DATA_POINTS
import com.zackjp.devicedx.permissions.AppPermission
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private val testDispatcher = StandardTestDispatcher()

    private val wifiDataSource = mockk<WifiDataSource>()
    private val appPermission = mockk<AppPermission>()
    private val realTimeNetworkDataSource = mockk<RealTimeNetworkDataSource>()

    private lateinit var viewModel: DashboardViewModel

    private val latencyMillisFlow = MutableSharedFlow<Long>()

    private companion object {
        val result1 = ScanResult().apply { SSID = "ssid-name-1" }
        val result2 = ScanResult().apply { SSID = "ssid-name-2" }
        val scanResults = listOf(result1, result2)
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { appPermission.hasFineLocation() } returns false
        every { wifiDataSource.getWifiScanFlow() } returns flowOf(scanResults)
        every { realTimeNetworkDataSource.getLatencyMillisFlow() } returns latencyMillisFlow
        viewModel = DashboardViewModel(
            appPermission = appPermission,
            realTimeNetworkDataSource = realTimeNetworkDataSource,
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
        every { appPermission.hasFineLocation() } returns true

        viewModel.onStartScan()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value
        state.wifiNames shouldBe listOf("ssid-name-1", "ssid-name-2")
    }

    @Test
    fun onStartScan_WithFineLocationAccess_SetsPermissionStatusToGranted() = runTest {
        initViewModel()
        every { appPermission.hasFineLocation() } returns true

        viewModel.onStartScan()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value
        state.permissionStatus shouldBe PermissionStatus.Granted
    }

    @Test
    fun onStartScan_WithFineLocationAccess_DoesNotEmitRequestPermissionEvent() = runTest {
        initViewModel()
        every { appPermission.hasFineLocation() } returns true

        viewModel.events.test {
            viewModel.onStartScan()
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun onStartScan_WithoutFineLocationAccess_EmitsRequestPermissionEventOnlyOnce() = runTest {
        initViewModel()
        every { appPermission.hasFineLocation() } returns false

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
        every { appPermission.hasFineLocation() } returns false

        viewModel.onStartScan()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value
        state.permissionStatus shouldBe PermissionStatus.Pending
    }

    @Test
    fun onFineLocationPermissionDenied_SetsPermissionStatusToDenied() = runTest {
        initViewModel()

        viewModel.screenState.test {
            viewModel.onFineLocationPermissionDenied()
            testDispatcher.scheduler.advanceUntilIdle()

            expectMostRecentItem().permissionStatus shouldBe PermissionStatus.Denied
        }
    }

    @Test
    fun onMonitorLatency_WhenWifiScanInProgress_CancelsWifiScan() = runTest {
        initViewModel()

        val scanResultsFlow = MutableSharedFlow<List<ScanResult>>()
        val scanResult1 = ScanResult().apply { SSID = "ssid-name-1" }
        val scanResult2 = ScanResult().apply { SSID = "ssid-name-2" }
        every { appPermission.hasFineLocation() } returns true // for onStartScan() permission check
        every { wifiDataSource.getWifiScanFlow() } returns scanResultsFlow

        viewModel.screenState.test {
            awaitItem().wifiNames shouldBe emptyList()

            viewModel.onStartScan()
            advanceUntilIdle()
            scanResultsFlow.emit(listOf(scanResult1))
            advanceUntilIdle()

            expectMostRecentItem().wifiNames shouldBe listOf("ssid-name-1")

            viewModel.onMonitorLatency()
            advanceUntilIdle()
            awaitItem().activeView shouldBe DashboardView.Latency

            scanResultsFlow.emit(listOf(scanResult2))
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun onMonitorLatency_UpdatesActiveViewToLatency() = runTest {
        initViewModel()

        viewModel.screenState.test {
            awaitItem().activeView shouldNotBe DashboardView.Latency

            viewModel.onMonitorLatency()
            advanceUntilIdle()

            awaitItem().activeView shouldBe DashboardView.Latency
        }
    }

    @Test
    fun onMonitorLatency_WhenLatencyValueEmitted_UpdatesLatencyHistory() = runTest {
        initViewModel()

        viewModel.screenState.test {
            skipItems(1) // initial state
            viewModel.onMonitorLatency()
            advanceUntilIdle()

            latencyMillisFlow.emit(11L)
            advanceUntilIdle()

            expectMostRecentItem().latencyHistory shouldBe listOf(11L)

            latencyMillisFlow.emit(13L)
            advanceUntilIdle()

            expectMostRecentItem().latencyHistory shouldBe listOf(11L, 13L)
        }
    }

    @Test
    fun onMonitorLatency_WhenLatencyValueEmitted_OnlyKeepsMaxHistory() = runTest {
        initViewModel()

        viewModel.screenState.test {
            skipItems(1) // initial state
            viewModel.onMonitorLatency()
            advanceUntilIdle()

            repeat(MAX_LATENCY_DATA_POINTS + 3) {
                latencyMillisFlow.emit(it.toLong())
                advanceUntilIdle()
            }

            val expectedHistory = (3..<3 + MAX_LATENCY_DATA_POINTS)
                .map { it.toLong() }
                .toList()
            expectMostRecentItem().latencyHistory shouldBe expectedHistory
        }
    }

    @Test
    fun stopActiveMonitor_WhenLatencyMonitorActive_StopsNewEmissions() = runTest {
        initViewModel()

        viewModel.screenState.test {
            viewModel.onMonitorLatency()
            advanceUntilIdle()
            expectMostRecentItem().latencyHistory shouldBe emptyList()

            latencyMillisFlow.emit(11L)
            advanceUntilIdle()
            expectMostRecentItem().latencyHistory shouldBe listOf(11L)

            viewModel.stopActiveMonitor()
            advanceUntilIdle()
            latencyMillisFlow.emit(13L)
            advanceUntilIdle()
            expectMostRecentItem().latencyHistory shouldBe listOf(11L)
        }
    }

    private fun TestScope.initViewModel() {
        viewModel.screenState.launchIn(backgroundScope)
    }

}