package com.zackjp.devicedx.feature.traffic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zackjp.devicedx.R
import com.zackjp.devicedx.model.Bytes.Companion.asDataUnit
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficSession
import com.zackjp.devicedx.shared.ui.AppCard
import com.zackjp.devicedx.ui.theme.MediumGray
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant


@Composable
internal fun SessionInfoCard(
    modifier: Modifier = Modifier,
    isActiveProvider: () -> Boolean,
    sessionStartTimeProvider: () -> Long?,
    trafficSessionProvider: () -> TrafficSession?,
) {
    val isActive = isActiveProvider()
    val sessionId = trafficSessionProvider()?.id
    val sessionStartTime = sessionStartTimeProvider()
    val totalRxBytes = trafficSessionProvider()?.totalRxBytes
    val totalTxBytes = trafficSessionProvider()?.totalTxBytes

    var sessionDuration by remember { mutableStateOf(Duration.ZERO) }
    LaunchedEffect(isActive, sessionStartTime) {
        if (sessionStartTime != null) {
            val startInstant = Instant.fromEpochMilliseconds(sessionStartTime)
            while (isActive) {
                val now = Clock.System.now()
                sessionDuration = now - startInstant
                delay(1000)
            }
        }
    }

    AppCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = stringResource(R.string.traffic_session_card_label),
            )

            Spacer(Modifier.height(12.dp))

            val sessionStats: List<Pair<String, String>> = listOf(
                stringResource(R.string.traffic_session_info_label_session_id) to (sessionId?.let {
                    stringResource(R.string.traffic_session_id_name, it)
                } ?: "-"),
                stringResource(R.string.traffic_session_info_label_duration) to formatDuration(sessionDuration),
                stringResource(R.string.traffic_session_info_label_total_incoming) to formatBytes(totalRxBytes),
                stringResource(R.string.traffic_session_info_label_total_outgoing) to formatBytes(totalTxBytes),
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sessionStats.forEach { (statLabel, statValue) ->
                    SessionInfoCell(
                        modifier = Modifier.weight(1f),
                        statLabel = statLabel,
                        statValue = statValue,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionInfoCell(
    modifier: Modifier = Modifier,
    statLabel: String,
    statValue: String,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            color = MediumGray,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            style = MaterialTheme.typography.labelSmall,
            text = statLabel,
        )
        Text(
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            style = MaterialTheme.typography.bodyLarge,
            text = statValue,
        )
    }
}

private fun formatDuration(sessionDuration: Duration): String =
    sessionDuration.toComponents { _, hours, minutes, seconds, _ ->
        when {
            hours == 0 -> String.format(
                Locale.current.platformLocale,
                "%02d:%02d",
                minutes,
                seconds,
            )

            else -> String.format(
                Locale.current.platformLocale,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds,
            )
        }
    }

private fun formatBytes(bytes: Long?): String =
    if (bytes == null) {
        "- MB"
    } else {
        val displayStat = bytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit
        val valueText = displayStat.first.toPlainString()
        val unitText = displayStat.second.displayString
        "$valueText $unitText"
    }
