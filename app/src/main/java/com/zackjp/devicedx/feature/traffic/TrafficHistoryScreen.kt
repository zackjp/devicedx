package com.zackjp.devicedx.feature.traffic

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.model.TrafficSession
import com.zackjp.devicedx.shared.ui.ScreenScaffold


@Composable
fun TrafficHistoryScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: TrafficHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(
        modifier = modifier,
    ) {
        ReadyContent(
            modifier = Modifier.padding(horizontal = 12.dp),
            sessions = state.sessions,
        )
    }
}

@Composable
fun ReadyContent(
    modifier: Modifier = Modifier,
    sessions: List<TrafficSession>
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(sessions, key = { it.id }) { session ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Session ${session.id}")
            }
        }
    }
}