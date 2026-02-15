package com.zackjp.devicedx.feature.dashboard

import android.net.wifi.ScanResult
import app.cash.turbine.test
import com.zackjp.devicedx.concurrency.TestDispatcherProvider
import com.zackjp.devicedx.data.RealTimeNetworkDataSource
import com.zackjp.devicedx.data.WifiDataSource
import com.zackjp.devicedx.feature.dashboard.DashboardViewModel.Companion.MAX_LATENCY_DATA_POINTS
import com.zackjp.devicedx.model.TrafficData
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.system.permissions.PermissionChecker
import io.kotest.matchers.collections.shouldContainInOrder
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class) // Dispatchers.setMain()/resetMain(), advanceUntilIdle()
class DashboardViewModelTest {

    private val testDispatcherProvider = TestDispatcherProvider()

    private val clock = mockk<Clock>()
    private val wifiDataSource = mockk<WifiDataSource>()
    private val permissionChecker = mockk<PermissionChecker>()
    private val realTimeNetworkDataSource = mockk<RealTimeNetworkDataSource>()

    private lateinit var viewModel: DashboardViewModel

    private val latencyMillisFlow = MutableSharedFlow<Long>()
    private val trafficStatsFlow = MutableSharedFlow<TrafficData>()

    private companion object {
        val result1 = ScanResult().apply { SSID = "ssid-name-1" }
        val result2 = ScanResult().apply { SSID = "ssid-name-2" }
        val scanResults = listOf(result1, result2)
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcherProvider.default)

        every { clock.now() } returns Instant.fromEpochMilliseconds(10)
        every { permissionChecker.hasFineLocation() } returns false
        every { wifiDataSource.getWifiScanFlow() } returns flowOf(scanResults)
        every { realTimeNetworkDataSource.getLatencyMillisFlow() } returns latencyMillisFlow
        every { realTimeNetworkDataSource.getTrafficStats() } returns trafficStatsFlow
        viewModel = DashboardViewModel(
            clock = clock,
            dispatcherProvider = testDispatcherProvider,
            permissionChecker = permissionChecker,
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

    @Test
    fun onMonitorLatency_WhenWifiScanInProgress_CancelsWifiScan() = runTest {
        initViewModel()

        val scanResultsFlow = MutableSharedFlow<List<ScanResult>>()
        val scanResult1 = ScanResult().apply { SSID = "ssid-name-1" }
        val scanResult2 = ScanResult().apply { SSID = "ssid-name-2" }
        every { permissionChecker.hasFineLocation() } returns true // for onStartScan() permission check
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

    @Test
    fun stopActiveMonitor_WhenTrafficMonitorActive_StopsNewEmissions() = runTest {
        initViewModel()

        val trafficMetricsTester = TrafficMetricsTester(this@runTest, trafficStatsFlow) { newTime ->
            every { clock.now() } returns Instant.fromEpochMilliseconds(newTime)
        }
        viewModel.screenState.test {

            viewModel.onMonitorTraffic()
            advanceUntilIdle()
            expectMostRecentItem().trafficMetrics shouldBe emptyList()

            trafficMetricsTester.emitNextTrafficWithMsDelay(deltaTime = 1000, rxBytesTotal = 7)
            trafficMetricsTester.emitNextTrafficWithMsDelay(deltaTime = 1000, rxBytesTotal = 11)
            val expectedMetrics = listOf(
                TrafficMetric(1000, 0f),
                TrafficMetric(2000, 4f),
            )
            expectMostRecentItem().trafficMetrics shouldContainInOrder expectedMetrics

            viewModel.stopActiveMonitor()
            advanceUntilIdle()
            trafficMetricsTester.emitNextTrafficWithMsDelay(deltaTime = 1000, rxBytesTotal = 13)
            expectMostRecentItem().trafficMetrics shouldContainInOrder expectedMetrics
        }
    }

    @Test
    fun onMonitorTraffic_UpdatesActiveViewToTraffic() = runTest {
        initViewModel()

        viewModel.screenState.test {
            awaitItem().activeView shouldNotBe DashboardView.Traffic

            viewModel.onMonitorTraffic()

            awaitItem().activeView shouldBe DashboardView.Traffic
        }
    }

    @Test
    fun onMonitorTraffic_WhenTrafficDataEmitted_UpdatesTrafficHistory() = runTest {
        initViewModel()

        val trafficMetricsTester = TrafficMetricsTester(this@runTest, trafficStatsFlow) { newTime ->
            every { clock.now() } returns Instant.fromEpochMilliseconds(newTime)
        }
        viewModel.screenState.test {
            trafficMetricsTester.emitNextTrafficWithMsDelay(1000, 7) // ignored, not monitored
            expectMostRecentItem().trafficMetrics shouldBe emptyList()

            viewModel.onMonitorTraffic()
            advanceUntilIdle()

            trafficMetricsTester.emitNextTrafficWithMsDelay(1000, 11) // ignored, no prev datapoint
            trafficMetricsTester.emitNextTrafficWithMsDelay(1000, 13) // delta = 2 bytes
            trafficMetricsTester.emitNextTrafficWithMsDelay(1000, 17) // delta = 4 bytes

            expectMostRecentItem().trafficMetrics shouldContainInOrder listOf(
                TrafficMetric(3000, 2f),
                TrafficMetric(4000, 4f),
            )
        }
    }

    private fun TestScope.initViewModel() {
        viewModel.screenState.launchIn(backgroundScope)
    }

    private class TrafficMetricsTester(
        private val testScope: TestScope,
        private val trafficStatsFlow: MutableSharedFlow<TrafficData>,
        private val clockSetter: (Long) -> Unit,
    ) {

        private var currentTime: Long = 0
        private var totalRxBytes: Long = 0

        suspend fun emitNextTrafficWithMsDelay(deltaTime: Long, rxBytesTotal: Long) {
            currentTime += deltaTime
            totalRxBytes = rxBytesTotal
            clockSetter(currentTime)

            trafficStatsFlow.emit(
                TrafficData(
                    timestamp = currentTime,
                    rxBytes = totalRxBytes,
                )
            )
            testScope.advanceUntilIdle()
        }

    }
}
