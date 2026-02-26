package com.zackjp.devicedx.feature.latency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.feature.latency.LatencyViewModel.Companion.MAX_LATENCY_DATA_POINTS
import com.zackjp.devicedx.shared.ui.Graph
import com.zackjp.devicedx.shared.ui.GraphEntry

@Composable
fun LatencyScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: LatencyViewModel = hiltViewModel(),
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()

    Surface(modifier) {
        LazyColumn {
            latencyGraph(
                latencyHistory = state.latencyHistory,
                modifier = Modifier.fillMaxWidth(),
            )

            item {
                val (textResId, onClick) = if (state.isMonitorActive)
                    R.string.latency_monitor_stop to viewModel::stopMonitor
                else
                    R.string.latency_monitor_start to viewModel::startMonitor
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(textResId))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.latencyGraph(
    latencyHistory: List<Long>,
    modifier: Modifier = Modifier,
) {
    item {
        Column(modifier) {
            Text(stringResource(R.string.latency_ms, latencyHistory.lastOrNull() ?: "-"))
            Graph(
                data = latencyHistory.mapIndexed { index, latency ->
                    GraphEntry(index.toFloat(), latency.toFloat())
                },
                getY = { if (it !in 0..latencyHistory.lastIndex) 0f else latencyHistory[it].toFloat() },
                getYTickLabel = { "${it.toInt()}ms" },
                maxDataPoints = MAX_LATENCY_DATA_POINTS,
                modifier = Modifier
                    .background(Color.Black)
                    .fillMaxWidth()
                    .aspectRatio(1.5f),
                unitScaleY = 1000,
            )
        }
    }
}
