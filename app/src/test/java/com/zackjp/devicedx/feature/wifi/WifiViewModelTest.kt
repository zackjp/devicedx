package com.zackjp.devicedx.feature.wifi

import android.net.wifi.ScanResult
import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.system.WifiInfo
import com.zackjp.devicedx.system.permissions.PermissionChecker
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

    private val wifiInfoFlow = MutableSharedFlow<WifiInfo>()

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
        every { wifiDataSource.getWifiInfo() } returns wifiInfoFlow
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
            val wifiInfo1 = WifiInfo(ipAddress = "ipAddress1", wifiStrength = 3)
            wifiInfoFlow.emit(wifiInfo1)
            advanceUntilIdle()
            expectMostRecentItem().wifiInfo shouldBe wifiInfo1

            val wifiInfo2 = WifiInfo(ipAddress = "ipAddress2", wifiStrength = 2)
            wifiInfoFlow.emit(wifiInfo2)
            advanceUntilIdle()
            expectMostRecentItem().wifiInfo shouldBe wifiInfo2
        }
    }

    @Test
    fun init_WhenScreenStateReachesZeroSubscribers_AutoPausesSignalStrengthEmissions() = runTest {
        val emulatedUiSubscription = viewModel.screenState.launchIn(backgroundScope)
        val expectedTimeoutMs = 5000L
        val expectedFinalEmission = WifiInfo(ipAddress = "info1", 1)
        val unexpectedFinalEmission = WifiInfo(ipAddress = "info2", 2)
        advanceUntilIdle()

        emulatedUiSubscription.cancel()
        advanceTimeBy(expectedTimeoutMs - 1)

        wifiInfoFlow.emit(expectedFinalEmission)
        runCurrent()
        viewModel.screenState.value.wifiInfo shouldBe expectedFinalEmission

        advanceTimeBy(1)
        wifiInfoFlow.emit(unexpectedFinalEmission)
        runCurrent()
        viewModel.screenState.value.wifiInfo shouldBe expectedFinalEmission
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
    fun onFineLocationPermissionResult_WhenGrantedAndHasFineLocation_AutoStartsMonitor() = runTest {
        initViewModel()

        every { permissionChecker.hasFineLocation() } returns true
        val expectedWifiNames = listOf(result1.SSID, result2.SSID)

        viewModel.screenState.test {
            expectMostRecentItem().should {
                it.permissionStatus shouldNotBe PermissionStatus.Granted
                it.wifiNames shouldNotBe expectedWifiNames
            }

            viewModel.onFineLocationPermissionResult(
                isGranted = true,
                shouldShowRationale = false,
            )
            advanceUntilIdle()

            expectMostRecentItem().should {
                it.permissionStatus shouldBe PermissionStatus.Granted
                it.wifiNames shouldBe expectedWifiNames
            }
        }
    }

    @Test
    fun onFineLocationPermissionResult_WhenGrantedAndWithoutFineLocation_DoesNotAutoStartMonitor() =
        runTest {
            initViewModel()

            every { permissionChecker.hasFineLocation() } returns false

            viewModel.screenState.test {
                viewModel.onFineLocationPermissionResult(
                    isGranted = true,
                    shouldShowRationale = false,
                )
                advanceUntilIdle()

                expectMostRecentItem().should {
                    it.permissionStatus shouldNotBe PermissionStatus.Granted
                    it.wifiNames shouldBe emptyList()
                }
            }
        }

    @Test
    fun onFineLocationPermissionResult_WhenNotGrantedAndShouldShowRationale_SetsDeniedTemporarily() =
        runTest {
            initViewModel()

            viewModel.screenState.test {
                viewModel.onFineLocationPermissionResult(
                    isGranted = false,
                    shouldShowRationale = true,
                )
                advanceUntilIdle()

                expectMostRecentItem().permissionStatus shouldBe PermissionStatus.DeniedTemporarily
            }
        }

    @Test
    fun onFineLocationPermissionResult_WhenNotGrantedAndShouldNotShowRationale_SetsDeniedPermanently() =
        runTest {
            initViewModel()

            viewModel.screenState.test {
                viewModel.onFineLocationPermissionResult(
                    isGranted = false,
                    shouldShowRationale = false,
                )
                advanceUntilIdle()

                expectMostRecentItem().permissionStatus shouldBe PermissionStatus.DeniedPermanently
            }
        }

    private fun TestScope.initViewModel() {
        viewModel.screenState.launchIn(backgroundScope)
        advanceUntilIdle()
    }
}
