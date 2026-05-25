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
import com.zackjp.devicedx.feature.traffic.model.TrafficDisplayInfo
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.shared.ui.AppCard
import com.zackjp.devicedx.ui.theme.MediumGray
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Duration


private val sessionStartFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

@Composable
internal fun SessionInfoCard(
    modifier: Modifier = Modifier,
    trafficDisplayInfoProvider: () -> TrafficDisplayInfo?,
) {
    val displayData = trafficDisplayInfoProvider()

    val sessionId = displayData?.session?.id
    val sessionStartTime = displayData?.session?.startTime
    val sessionEndTime = displayData?.session?.endTime
    val rxValue = displayData?.totalRxValue
    val txValue = displayData?.totalTxValue
    val rxUnit = displayData?.totalRxUnit
    val txUnit = displayData?.totalTxUnit

    var sessionDuration by remember { mutableStateOf(Duration.ZERO) }
    LaunchedEffect(sessionEndTime, sessionStartTime) {
        val nonNullStartTime = sessionStartTime ?: return@LaunchedEffect
        val startInstant = kotlin.time.Instant.fromEpochMilliseconds(nonNullStartTime)
        if (sessionEndTime == null) {
            while (true) {
                val now = Clock.System.now()
                sessionDuration = now - startInstant
                delay(1000)
            }
        } else {
            val endInstant = kotlin.time.Instant.fromEpochMilliseconds(sessionEndTime)
            sessionDuration = endInstant - startInstant
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
            val title = sessionId?.let { stringResource(R.string.traffic_session_id_name, it) }
                ?: stringResource(R.string.traffic_session_card_label)
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = title,
            )

            Spacer(Modifier.height(12.dp))

            val sessionStats: List<Pair<String, String>> = listOf(
                stringResource(R.string.traffic_session_info_label_session_start) to formatStartTime(
                    sessionStartTime
                ),
                stringResource(R.string.traffic_session_info_label_duration) to formatDuration(
                    sessionDuration
                ),
                stringResource(R.string.traffic_session_info_label_total_incoming) to formatBytes(
                    rxValue,
                    rxUnit
                ),
                stringResource(R.string.traffic_session_info_label_total_outgoing) to formatBytes(
                    txValue,
                    txUnit
                ),
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

private fun formatStartTime(startTime: Long?): String {
    val nonNullStartTime = startTime ?: return "-"

    val instant = Instant.ofEpochMilli(nonNullStartTime)
    val date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

    return date.format(sessionStartFormatter)
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

private fun formatBytes(valueText: BigDecimal?, unit: DataUnit?): String =
    if (valueText == null || unit == null) {
        "-"
    } else {
        "$valueText ${unit.displayString}"
    }
