package com.zackjp.devicedx.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import app.cash.turbine.test
import com.zackjp.devicedx.system.ReceiverManager
import com.zackjp.devicedx.system.WifiInfo
import com.zackjp.devicedx.system.WifiManagerWrapper
import com.zackjp.devicedx.system.permissions.PermissionChecker
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.invoke
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


@OptIn(ExperimentalCoroutinesApi::class) // Dispatchers.setMain()/resetMain(), advanceUntilIdle()
class WifiDataSourceTest {

    private val testDispatcher = StandardTestDispatcher()

    private val permissionChecker = mockk<PermissionChecker>()
    private val receiverManager = mockk<ReceiverManager>()
    private val wifiManagerWrapper = mockk<WifiManagerWrapper>()

    private lateinit var dataSource: WifiDataSource

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { wifiManagerWrapper.requestScan() } just runs
        every { receiverManager.unregisterReceiver(any()) } just runs

        dataSource = WifiDataSource(
            permissionChecker = permissionChecker,
            appScope = TestScope(testDispatcher),
            receiverManager = receiverManager,
            wifiManagerWrapper = wifiManagerWrapper,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getWifiStrengthFlow_EmitsSignalStrength() = runTest(testDispatcher) {
        dataSource = WifiDataSource(
            permissionChecker = permissionChecker,
            appScope = backgroundScope,
            receiverManager = receiverManager,
            wifiManagerWrapper = wifiManagerWrapper,
        )
        val wifiInfoItems = listOf(
            WifiInfo(wifiStrength = 3),
            WifiInfo(wifiStrength = 4),
            WifiInfo(wifiStrength = 1),
            WifiInfo(wifiStrength = 2),
        )
        every { wifiManagerWrapper.getWifiInfo() } returnsMany wifiInfoItems

        val wifiStrengthFlow = dataSource.getWifiInfo()

        wifiStrengthFlow.test {
            awaitItem() shouldBe WifiInfo()
            awaitItem() shouldBe wifiInfoItems[0]
            awaitItem() shouldBe wifiInfoItems[1]
            awaitItem() shouldBe wifiInfoItems[2]
            awaitItem() shouldBe wifiInfoItems[3]

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun wifiScanResults_WhenSystemEmitsScanResults_FlowAlsoEmits() = runTest(testDispatcher) {
        val scanResultsIntent = Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        val broadcastReceiver = mockk<BroadcastReceiver>()
        val expectedScanResults = listOf(mockk<ScanResult>(), mockk<ScanResult>())
        val receiverLambda = slot<(Context?, Intent?) -> Unit>()

        every { wifiManagerWrapper.getCachedScanResults() } returns Result.success(expectedScanResults)
        every { receiverManager.registerReceiver(any(), capture(receiverLambda)) } returns broadcastReceiver
        every { permissionChecker.hasFineLocation() } returns true
        every { permissionChecker.hasWifiState() } returns true

        dataSource.getWifiScanFlow().test {
            awaitItem() shouldBe emptyList()
            testDispatcher.scheduler.advanceUntilIdle()

            receiverLambda.invoke(null, scanResultsIntent)

            awaitItem() shouldBe expectedScanResults
        }
    }

}
