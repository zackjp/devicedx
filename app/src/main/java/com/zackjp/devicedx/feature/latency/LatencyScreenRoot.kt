package com.zackjp.devicedx.feature.latency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.shared.ui.Graph
import com.zackjp.devicedx.shared.ui.GraphEntry
import com.zackjp.devicedx.shared.ui.LineConfig
import com.zackjp.devicedx.shared.ui.getScaleCount
import kotlin.math.max

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

            val unitScaleY = 1000
            val xMinValue = 0L
            val xMaxValue = latencyHistory.lastIndex.toLong()
            var yMaxValue = 0L
            latencyHistory.forEach {
                yMaxValue = max(yMaxValue, it)
            }

            val yAxisScale = yMaxValue.getScaleCount(unitScaleY)
            val yTickMaxValue = unitScaleY.toBigDecimal().pow(yAxisScale).toLong()

            Graph(
                lines = listOf(
                    LineConfig(
                        data = latencyHistory.mapIndexed { index, latency ->
                            GraphEntry(index.toLong(), latency)
                        },
                        color = Color.Magenta,
                    )
                ),
                getYTickLabel = { "${it.toInt()}ms" },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black)
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .padding(12.dp),
                xTickStartValue = xMinValue,
                xTickEndValue = xMaxValue,
                yTickBottomValue = 0L,
                yTickTopValue = yTickMaxValue,
                yTickCount = 4,
            )
        }
    }
}
