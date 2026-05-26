package com.zackjp.devicedx.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.zackjp.devicedx.feature.dashboard.DashboardScreen
import com.zackjp.devicedx.feature.latency.LatencyScreenRoot
import com.zackjp.devicedx.feature.traffic.TrafficHistoryScreenRoot
import com.zackjp.devicedx.feature.traffic.TrafficMonitorScreenRoot
import com.zackjp.devicedx.feature.traffic.TrafficViewModel
import com.zackjp.devicedx.feature.wifi.WifiScreenRoot

val TRANSITION_ANIM_FORWARD: AnimatedContentTransitionScope<Scene<Any>>.() -> ContentTransform =
    { EnterTransition.None togetherWith ExitTransition.None }
val TRANSITION_ANIM_POP: AnimatedContentTransitionScope<Scene<Any>>.() -> ContentTransform =
    { EnterTransition.None togetherWith slideOutHorizontally { it } }
val TRANSITION_ANIM_PREDICTIVE_POP: AnimatedContentTransitionScope<Scene<Any>>.(Int) -> ContentTransform =
    { EnterTransition.None togetherWith slideOutHorizontally { it } }

@Composable
fun DeviceDxNav3Graph(
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Route.Dashboard)
    val navActions = remember {
        NavActions(
            toDashboard = { backStack.add(Route.Dashboard) },
            toLatencyMonitor = { backStack.add(Route.LatencyMonitor) },
            toTrafficMonitor = { backStack.add(Route.TrafficMonitor()) },
            toTrafficHistory = {
                backStack.removeAll { it is Route.TrafficHistory }
                backStack.add(Route.TrafficHistory)
            },
            toWifiMonitor = { backStack.add(Route.WifiMonitor) }
        )
    }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        transitionSpec = TRANSITION_ANIM_FORWARD,
        popTransitionSpec = TRANSITION_ANIM_POP,
        predictivePopTransitionSpec = TRANSITION_ANIM_PREDICTIVE_POP,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Route.Dashboard> {
                DashboardScreen(
                    modifier = Modifier
                        .fillMaxWidth(),
                    navActions = navActions,
                )
            }

            entry<Route.LatencyMonitor> {
                LatencyScreenRoot(
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }

            entry<Route.TrafficMonitor> { key ->
                val trafficViewModel: TrafficViewModel = hiltViewModel(
                    creationCallback = { factory: TrafficViewModel.Factory ->
                        factory.create(key.sessionId)
                    }
                )

                TrafficMonitorScreenRoot(
                    modifier = Modifier
                        .fillMaxWidth(),
                    navActions = navActions,
                    viewModel = trafficViewModel,
                )
            }

            entry<Route.TrafficHistory>(
                clazzContentKey = { Route.TrafficHistory::class.qualifiedName!! },
            ) {
                TrafficHistoryScreenRoot(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onNavigateToSession = { sessionId ->
                        backStack.removeAll { it is Route.TrafficMonitor || it is Route.TrafficHistory }
                        backStack.add(Route.TrafficMonitor(sessionId))
                    }
                )
            }

            entry<Route.WifiMonitor> {
                WifiScreenRoot(
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
        },
    )
}
