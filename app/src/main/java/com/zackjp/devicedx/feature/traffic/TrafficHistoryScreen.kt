package com.zackjp.devicedx.feature.traffic

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.feature.traffic.model.TrafficDisplayInfo
import com.zackjp.devicedx.feature.traffic.ui.SessionInfoCard
import com.zackjp.devicedx.shared.ui.ScreenScaffold


@Composable
fun TrafficHistoryScreenRoot(
    modifier: Modifier = Modifier,
    onNavigateToSession: (Long) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    viewModel: TrafficHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(
        modifier = modifier,
    ) {
        ReadyContent(
            modifier = Modifier.padding(horizontal = 12.dp),
            animatedVisibilityScope = animatedVisibilityScope,
            onNavigateToSession = onNavigateToSession,
            trafficDiplayInfoList = state.sessions,
            sharedTransitionScope = sharedTransitionScope,
        )
    }
}

@Composable
private fun ReadyContent(
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToSession: (Long) -> Unit,
    trafficDiplayInfoList: List<TrafficDisplayInfo>,
    sharedTransitionScope: SharedTransitionScope
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(trafficDiplayInfoList, key = { it.session.id }) { trafficDisplayInfo ->
            with(sharedTransitionScope) {
                SessionInfoCard(
                    modifier = Modifier
                        .sharedElement(
                            sharedContentState = sharedTransitionScope.rememberSharedContentState(
                                "session-${trafficDisplayInfo.session.id}",
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .fillMaxWidth()
                        .clickable { onNavigateToSession(trafficDisplayInfo.session.id) },
                    trafficDisplayInfoProvider = { trafficDisplayInfo },
                )
            }
        }
    }
}
