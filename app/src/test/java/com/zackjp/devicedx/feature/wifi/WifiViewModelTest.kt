package com.zackjp.devicedx.feature.wifi

import android.net.wifi.ScanResult
import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.system.permissions.PermissionChecker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


@OptIn(ExperimentalCoroutinesApi::class)
class WifiViewModelTest {

    private val testDispatcherProvider = TestDispatcherProvider()
    private val wifiDataSource = mockk<WifiDataSource>()
    private val permissionChecker = mockk<PermissionChecker>()

    private val wifiStrengthFlow = MutableSharedFlow<Int>()

    private lateinit var viewModel: WifiViewModel

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
        every { wifiDataSource.getWifiStrengthFlow() } returns wifiStrengthFlow
        viewModel = WifiViewModel(
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
    fun init_RegistersWifiSignalStrengthListener() = runTest {
        initViewModel()

        viewModel.screenState.test {
            wifiStrengthFlow.emit(3)
            advanceUntilIdle()
            expectMostRecentItem().wifiStrength shouldBe 3

            wifiStrengthFlow.emit(2)
            advanceUntilIdle()
            expectMostRecentItem().wifiStrength shouldBe 2
        }
    }

    @Test
    fun init_WhenScreenStateReachesZeroSubscribers_AutoPausesSignalStrengthEmissions() = runTest {
        val emulatedUiSubscription = viewModel.screenState.launchIn(backgroundScope)
        val expectedTimeoutMs = 5000L
        val expectedFinalEmission = 1
        val unexpectedFinalEmission = 2
        advanceUntilIdle()

        emulatedUiSubscription.cancel()
        advanceTimeBy(expectedTimeoutMs - 1)

        wifiStrengthFlow.emit(expectedFinalEmission)
        runCurrent()
        viewModel.screenState.value.wifiStrength shouldBe expectedFinalEmission

        advanceTimeBy(1)
        wifiStrengthFlow.emit(unexpectedFinalEmission)
        runCurrent()
        viewModel.screenState.value.wifiStrength shouldBe expectedFinalEmission
    }

    @Test
    fun startMonitor_SetsMonitorActiveToTrue() = runTest {
        initViewModel()

        every { permissionChecker.hasFineLocation() } returns true

        viewModel.screenState.test {
            expectMostRecentItem().isMonitorActive shouldBe false

            viewModel.startMonitor()
            advanceUntilIdle()

            expectMostRecentItem().isMonitorActive shouldBe true
        }
    }

    @Test
    fun startMonitor_WithFineLocationAccess_TransformsMonitorResultsToWifiNames() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.startMonitor()
        advanceUntilIdle()

        val state = viewModel.screenState.value
        state.wifiNames shouldBe listOf("ssid-name-1", "ssid-name-2")
    }

    @Test
    fun startMonitor_WithFineLocationAccess_SetsPermissionStatusToGranted() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.startMonitor()
        advanceUntilIdle()

        val state = viewModel.screenState.value
        state.permissionStatus shouldBe PermissionStatus.Granted
    }

    @Test
    fun startMonitor_WithFineLocationAccess_DoesNotEmitRequestPermissionEvent() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.events.test {
            viewModel.startMonitor()
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun startMonitor_WithoutFineLocationAccess_EmitsRequestPermissionEventOnlyOnce() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns false

        viewModel.events.test {
            viewModel.startMonitor()
            advanceUntilIdle()
            awaitItem() shouldBe WifiScreenEvent.LaunchFineLocation

            viewModel.startMonitor()
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun startMonitor_WithoutFineLocationAccess_SetsPermissionStatusToPending() = runTest {
        initViewModel()
        every { permissionChecker.hasFineLocation() } returns false

        viewModel.startMonitor()
        advanceUntilIdle()

        val state = viewModel.screenState.value
        state.permissionStatus shouldBe PermissionStatus.Pending
    }

    @Test
    fun stopMonitor_SetsMonitorActiveToFalse() = runTest {
        initViewModel()

        every { permissionChecker.hasFineLocation() } returns true

        viewModel.screenState.test {
            viewModel.startMonitor()
            advanceUntilIdle()

            expectMostRecentItem().isMonitorActive shouldBe true

            viewModel.stopMonitor()
            advanceUntilIdle()

            expectMostRecentItem().isMonitorActive shouldBe false
        }
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
        advanceUntilIdle()
    }
}
