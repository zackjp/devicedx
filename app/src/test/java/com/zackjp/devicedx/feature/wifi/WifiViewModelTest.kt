package com.zackjp.devicedx.feature.wifi

import android.net.wifi.ScanResult
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.system.WifiInfo
import com.zackjp.devicedx.system.permissions.PermissionChecker
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.orbitmvi.orbit.test.OrbitTestContext
import org.orbitmvi.orbit.test.test


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
        viewModel.test(
            testScope = this,
        ) {
            initOrbitViewModel()
            runCurrent()

            val wifiInfo1 = WifiInfo(ipAddress = "ipAddress1", wifiStrength = 3)
            wifiInfoFlow.emit(wifiInfo1)
            awaitState().wifiInfo shouldBe wifiInfo1

            val wifiInfo2 = WifiInfo(ipAddress = "ipAddress2", wifiStrength = 2)
            wifiInfoFlow.emit(wifiInfo2)
            awaitState().wifiInfo shouldBe wifiInfo2

            verifyNoMoreItems()
        }
    }

    @Test
    fun startMonitor_WithFineLocationAccess_TransformsMonitorResultsToWifiNames() = runTest {
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.test(
            testScope = this,
        ) {
            initOrbitViewModel()

            containerHost.startMonitor()
            runCurrent()

            awaitState().wifiNames shouldBe emptyList()
            awaitState().wifiNames shouldBe listOf("ssid-name-1", "ssid-name-2")

            verifyNoMoreItems()
        }
    }

    @Test
    fun startMonitor_WithFineLocationAccess_SetsPermissionStatusToGranted() = runTest {
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.test(
            testScope = this,
        ) {
            initOrbitViewModel()

            containerHost.startMonitor()

            awaitState().permissionStatus shouldBe PermissionStatus.Granted

            verifyNoMoreItems()
        }
    }

    @Test
    fun startMonitor_WithFineLocationAccess_DoesNotEmitRequestPermissionEvent() = runTest {
        every { permissionChecker.hasFineLocation() } returns true

        viewModel.test(
            testScope = this,
        ) {
            initOrbitViewModel()

            containerHost.startMonitor()

            awaitItem() shouldNot beInstanceOf<WifiScreenEffect>()

            verifyNoMoreItems()
        }
    }

    @Test
    fun startMonitor_WithoutFineLocationAccess_EmitsRequestPermissionEventOnlyOnce() = runTest {
        every { permissionChecker.hasFineLocation() } returns false

        viewModel.test(
            testScope = this,
        ) {
            initOrbitViewModel()

            containerHost.startMonitor()
            awaitState().permissionStatus shouldBe PermissionStatus.Pending
            awaitSideEffect() shouldBe WifiScreenEffect.LaunchFineLocation

            containerHost.startMonitor()

            verifyNoMoreItems()
        }
    }

    @Test
    fun startMonitor_WithoutFineLocationAccess_SetsPermissionStatusToPendingAndRequestsAccess() = runTest {
        every { permissionChecker.hasFineLocation() } returns false

        viewModel.test(
            testScope = this,
        ) {
            initOrbitViewModel()

            viewModel.startMonitor()

            awaitState().permissionStatus shouldBe PermissionStatus.Pending
            awaitSideEffect() shouldBe WifiScreenEffect.LaunchFineLocation
            verifyNoMoreItems()
        }
    }

    @Test
    fun onFineLocationPermissionResult_WhenGrantedAndHasFineLocationWhilePending_AutoStartsMonitor() = runTest {
        every { permissionChecker.hasFineLocation() } returns true
        val initialWifiNames = listOf("initial-wifi-1", "initial-wifi-2")
        val expectedWifiNames = listOf(result1.SSID, result2.SSID)

        viewModel.test(
            testScope = this,
            initialState = WifiScreenState(
                permissionStatus = PermissionStatus.Pending,
                wifiNames = initialWifiNames,
                wifiInfo = WifiInfo(),
            ),
        ) {
            initOrbitViewModel()

            containerHost.onFineLocationPermissionResult(
                isGranted = true,
                shouldShowRationale = false,
            )

            awaitState().should {
                it.permissionStatus shouldBe PermissionStatus.Granted
                it.wifiNames shouldBe initialWifiNames
            }
            awaitState().should {
                it.permissionStatus shouldBe PermissionStatus.Granted
                it.wifiNames shouldBe expectedWifiNames
            }
            verifyNoMoreItems()
        }
    }

    @Test
    fun onFineLocationPermissionResult_WhenGrantedAndWithoutFineLocationWhileUnknown_RetriesFineLocationRequest() =
        runTest {
            every { permissionChecker.hasFineLocation() } returns false

            viewModel.test(
                testScope = this,
            ) {
                containerHost.onFineLocationPermissionResult(
                    isGranted = true,
                    shouldShowRationale = false,
                )
                advanceUntilIdle()

                awaitState().should {
                    it.permissionStatus shouldBe PermissionStatus.Pending
                    it.wifiNames shouldBe emptyList()
                }
                awaitSideEffect() shouldBe WifiScreenEffect.LaunchFineLocation
            }
        }

    @Test
    fun onFineLocationPermissionResult_WhenGrantedAndWithoutFineLocationWhilePending_DoesNothing() =
        runTest {
            every { permissionChecker.hasFineLocation() } returns false

            viewModel.test(
                testScope = this,
                initialState = WifiScreenState(
                    permissionStatus = PermissionStatus.Pending,
                    wifiNames = emptyList(),
                    wifiInfo = WifiInfo()
                ),
            ) {
                initOrbitViewModel()

                containerHost.onFineLocationPermissionResult(
                    isGranted = true,
                    shouldShowRationale = false,
                )

                verifyNoMoreItems()
            }
        }

    @Test
    fun onFineLocationPermissionResult_WhenNotGrantedAndShouldShowRationale_SetsDeniedTemporarily() =
        runTest {
            viewModel.test(
                testScope = this,
            ) {
                containerHost.onFineLocationPermissionResult(
                    isGranted = false,
                    shouldShowRationale = true,
                )
                advanceUntilIdle()

                awaitState().permissionStatus shouldBe PermissionStatus.DeniedTemporarily
            }
        }

    @Test
    fun onFineLocationPermissionResult_WhenNotGrantedAndShouldNotShowRationale_SetsDeniedPermanently() =
        runTest {
            viewModel.test(
                testScope = this,
            ) {
                containerHost.onFineLocationPermissionResult(
                    isGranted = false,
                    shouldShowRationale = false,
                )
                advanceUntilIdle()

                awaitState().permissionStatus shouldBe PermissionStatus.DeniedPermanently
            }
        }

    private fun OrbitTestContext<WifiScreenState, WifiScreenEffect, WifiViewModel>.initOrbitViewModel() {
        runOnCreate()
    }

    private suspend fun OrbitTestContext<WifiScreenState, WifiScreenEffect, WifiViewModel>.verifyNoMoreItems() {
        expectNoItems()
        // cancels the infinite flow(s) set up in container.onCreate.repeatOnSubscription{}.
        // otherwise, Orbit would throw an exception for unclosed flows
        cancelAndIgnoreRemainingItems()
    }

}
