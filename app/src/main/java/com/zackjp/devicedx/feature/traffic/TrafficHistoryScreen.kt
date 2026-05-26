package com.zackjp.devicedx.feature.traffic

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
    viewModel: TrafficHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(
        modifier = modifier,
    ) {
        ReadyContent(
            modifier = Modifier.padding(horizontal = 12.dp),
            onNavigateToSession = onNavigateToSession,
            sessionDiplayInfoList = state.sessions,
        )
    }
}

@Composable
private fun ReadyContent(
    modifier: Modifier = Modifier,
    onNavigateToSession: (Long) -> Unit,
    sessionDiplayInfoList: List<TrafficDisplayInfo>
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sessionDiplayInfoList, key = { it.session.id }) { sessionDisplayInfo ->
            SessionInfoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSession(sessionDisplayInfo.session.id) },
                trafficDisplayInfoProvider = { sessionDisplayInfo },
            )
        }
    }
}
