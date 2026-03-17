package com.zackjp.devicedx.feature.traffic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zackjp.devicedx.R
import com.zackjp.devicedx.model.Bytes.Companion.asDataUnit
import com.zackjp.devicedx.model.DataUnit
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.shared.ui.AppCard
import com.zackjp.devicedx.ui.theme.Platinum


@Composable
internal fun ThroughputRow(
    modifier: Modifier = Modifier,
    mostRecentStat: TrafficMetric?,
    rxLineColor: Color,
    txLineColor: Color,
) {
    Row(modifier = modifier) {
        ThroughputCard(
            modifier = Modifier.weight(1f),
            cardColor = rxLineColor,
            bytes = mostRecentStat?.rxBytesPerSec,
            label = stringResource(R.string.traffic_stat_label_rx),
        )
        Spacer(Modifier.width(8.dp))
        ThroughputCard(
            modifier = Modifier.weight(1f),
            cardColor = txLineColor,
            bytes = mostRecentStat?.txBytesPerSec,
            label = stringResource(R.string.traffic_stat_label_tx),
        )
    }
}

@Composable
fun ThroughputCard(
    modifier: Modifier = Modifier,
    cardColor: Color,
    bytes: Long?,
    label: String,
) {
    AppCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )

            val valueText: String
            val unitText: String
            if (bytes == null) {
                valueText = "-"
                unitText = "MB/s"
            } else {
                val displayStat = bytes.asDataUnit(DataUnit.BYTE).bestDisplayableUnit
                valueText = displayStat.first.toPlainString()
                unitText = displayStat.second.displayString + "/s"
            }

            val formattedStat = buildAnnotatedString {
                pushStyle(style = SpanStyle(color = cardColor))
                withStyle(style = MaterialTheme.typography.headlineMedium.toSpanStyle()) {
                    append(valueText)
                }
                append(" ")
                withStyle(style = MaterialTheme.typography.headlineSmall.toSpanStyle()) {
                    append(unitText)
                }
            }

            Text(
                modifier = Modifier,
                color = Platinum,
                text = formattedStat,
            )
        }
    }
}
