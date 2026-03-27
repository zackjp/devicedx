package com.zackjp.devicedx.feature.traffic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zackjp.devicedx.R
import com.zackjp.devicedx.model.Bytes.Companion.asDataUnit
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.ui.theme.Jade
import com.zackjp.devicedx.ui.theme.MediumGray
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


private val LIVE_COLOR = Jade
private val NOT_LIVE_COLOR = MediumGray

private val columnWeights = listOf(
    0.3f,
    0.35f,
    0.35f,
)
private val columnSpacing = 8.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TrafficTimeline(
    isSessionActiveProvider: () -> Boolean,
    metricsProvider: () -> List<TrafficMetric>,
    modifier: Modifier = Modifier,
    rxColor: Color,
    txColor: Color,
) {
    val metrics = metricsProvider()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val isNearTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex <= 1
        }
    }

    LaunchedEffect(metrics) {
        if (isNearTop) {
            lazyListState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier,
    ) {
        Title(
            modifier = Modifier.fillMaxWidth(),
            isLive = isNearTop,
            isSessionActive = isSessionActiveProvider(),
            onLiveClicked = { coroutineScope.launch { lazyListState.scrollToItem(0) } },
        )

        Spacer(Modifier.height(8.dp))

        TimelineHeader(
            modifier = Modifier.fillMaxWidth(),
        )
        TimelineList(
            modifier = Modifier.fillMaxWidth(),
            lazyListState = lazyListState,
            metrics = metrics,
            rxColor = rxColor,
            txColor = txColor,
        )
    }
}

@Composable
private fun Title(
    modifier: Modifier = Modifier,
    isLive: Boolean,
    isSessionActive: Boolean,
    onLiveClicked: () -> Unit = {},
) {
    Row(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            text = stringResource(R.string.traffic_timeline_title),
        )
        if (isSessionActive) {
            LiveStatus(
                modifier = Modifier
                    .clickable(onClick = onLiveClicked),
                isLive = isLive,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun LiveStatus(
    modifier: Modifier = Modifier,
    isLive: Boolean,
) {
    val color = if (isLive) LIVE_COLOR else NOT_LIVE_COLOR

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            modifier = Modifier.size(6.dp)
        ) {
            drawCircle(color)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            color = color,
            text = stringResource(R.string.traffic_timeline_live),
        )
    }
}

@Composable
private fun TimelineHeader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
    ) {
        Text(
            color = MediumGray,
            modifier = Modifier.weight(columnWeights[0]),
            style = MaterialTheme.typography.labelSmall,
            text = stringResource(R.string.traffic_timeline_label_timestamp),
        )
        Spacer(modifier = Modifier.width(columnSpacing))
        Text(
            color = MediumGray,
            modifier = Modifier.weight(columnWeights[1]),
            style = MaterialTheme.typography.labelSmall,
            text = stringResource(R.string.traffic_timeline_label_incoming),
        )
        Spacer(modifier = Modifier.width(columnSpacing))
        Text(
            color = MediumGray,
            modifier = Modifier.weight(columnWeights[2]),
            style = MaterialTheme.typography.labelSmall,
            text = stringResource(R.string.traffic_timeline_label_outgoing),
        )
    }
}

@Composable
private fun TimelineList(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    metrics: List<TrafficMetric>,
    rxColor: Color,
    txColor: Color,
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(metrics, key = { it.timestamp }) { metric ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier.weight(columnWeights[0]),
                    text = formatTimestamp(metric.timestamp),
                )
                Spacer(modifier = Modifier.width(columnSpacing))
                Text(
                    color = rxColor,
                    modifier = Modifier.weight(columnWeights[1]),
                    text = formatBytes(metric.rxBytesPerSec),
                )
                Spacer(modifier = Modifier.width(columnSpacing))
                Text(
                    color = txColor,
                    modifier = Modifier.weight(columnWeights[2]),
                    text = formatBytes(metric.txBytesPerSec),
                )
            }

        }
    }
}

private fun formatBytes(bytes: Long?): String =
    if (bytes == null) {
        "-"
    } else {
        val displayStat = bytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit
        val valueText = displayStat.first.toPlainString()
        val unitText = displayStat.second.displayString
        "$valueText $unitText"
    }

private fun formatTimestamp(millis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("hh:mm:ss")
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(millis),
        ZoneId.systemDefault(),
    )
    return dateTime.format(formatter)
}