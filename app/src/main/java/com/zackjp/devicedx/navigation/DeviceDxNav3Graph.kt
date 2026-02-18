package com.zackjp.devicedx.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.zackjp.devicedx.feature.dashboard.DashboardScreen
import com.zackjp.devicedx.feature.latency.LatencyScreenRoot
import com.zackjp.devicedx.feature.traffic.TrafficMonitorScreenRoot
import com.zackjp.devicedx.feature.wifi.WifiScreenRoot

@Composable
fun DeviceDxNav3Graph(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val backStack = SnapshotStateList<NavKey>(1) { Route.Dashboard }
    val navActions = NavActions(
        toDashboard = { backStack.add(Route.Dashboard) },
        toLatencyMonitor = { backStack.add(Route.LatencyMonitor) },
        toTrafficMonitor = { backStack.add(Route.TrafficMonitor) },
        toWifiMonitor = { backStack.add(Route.WifiMonitor) }
    )

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Route.Dashboard> {
                DashboardScreen(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(innerPadding)
                        .fillMaxWidth(),
                    navActions = navActions,
                )
            }

            entry<Route.LatencyMonitor> {
                LatencyScreenRoot(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(innerPadding)
                        .fillMaxWidth(),
                )
            }

            entry<Route.TrafficMonitor> {
                TrafficMonitorScreenRoot(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(innerPadding)
                        .fillMaxWidth(),
                )
            }

            entry<Route.WifiMonitor> {
                WifiScreenRoot(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(innerPadding)
                        .fillMaxWidth(),
                )
            }
        },
    )
}
