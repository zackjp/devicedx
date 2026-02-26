package com.zackjp.devicedx.feature.traffic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.feature.traffic.TrafficViewModel.Companion.TRAFFIC_METRICS_WINDOW_SECS
import com.zackjp.devicedx.model.TrafficMetric
import com.zackjp.devicedx.shared.ui.Graph
import com.zackjp.devicedx.shared.ui.GraphEntry
import com.zackjp.devicedx.shared.ui.rememberIsInPipMode
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val ASPECT_RATIO_NUMERATOR = 16
private const val ASPECT_RATIO_DENOMINATOR = 9
private const val ASPECT_RATIO_FLOAT = ASPECT_RATIO_NUMERATOR.toFloat() / ASPECT_RATIO_DENOMINATOR

private val RxLineColor = Color.Magenta

@Composable
fun TrafficMonitorScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: TrafficViewModel = hiltViewModel()
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()

    val isInPipMode = rememberIsInPipMode(
        isAllowed = state.isMonitorActive,
        aspectRatioNumerator = ASPECT_RATIO_NUMERATOR,
        aspectRatioDenominator = ASPECT_RATIO_DENOMINATOR,
    )

    Surface(
        if (isInPipMode) Modifier else modifier
    ) {
        TrafficMonitorScreen(
            isInPipMode = isInPipMode,
            modifier = Modifier.fillMaxWidth(),
            onStartMonitor = { viewModel.startMonitor() },
            onStopMonitor = { viewModel.stopMonitor() },
            state = state,
        )
    }
}

@Composable
private fun TrafficMonitorScreen(
    isInPipMode: Boolean,
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit = {},
    onStopMonitor: () -> Unit = {},
    state: TrafficScreenState,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        TrafficGraphCard(
            trafficMetrics = state.trafficMetrics,
            rxLineColor = RxLineColor,
            modifier = Modifier
                .background(Color.Black)
                .fillMaxWidth()
                .aspectRatio(ASPECT_RATIO_FLOAT),
        )

        Spacer(Modifier.height(12.dp))

        val trafficMetrics = state.trafficMetrics
        val mostRecentStat = trafficMetrics.lastOrNull()
        TrafficStat(
            modifier = Modifier.fillMaxWidth(),
            rxBytes = mostRecentStat?.rxBytesPerSec
        )

        if (!isInPipMode) {
            val (textResId, onClick) = if (state.isMonitorActive) {
                R.string.stop_traffic_monitor to onStopMonitor
            } else {
                R.string.start_traffic_monitor to onStartMonitor
            }
            Button(onClick = onClick) {
                Text(stringResource(textResId))
            }
        }
    }

}

@Composable
fun TrafficStat(
    rxBytes: Float?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Current Incoming",
                style = MaterialTheme.typography.bodySmall,
            )

            val rxValueText: String
            val rxUnitText: String
            if (rxBytes == null) {
                rxValueText = "-"
                rxUnitText = "MB/s"
            } else {
                val displayStat = getBytesString(rxBytes)
                rxValueText = displayStat.first.toPlainString()
                rxUnitText = displayStat.second + "/s"
            }

            val formattedStat = buildAnnotatedString {
                withStyle(style = MaterialTheme.typography.headlineMedium.toSpanStyle()) {
                    append(rxValueText)
                }
                append(" ")
                withStyle(style = MaterialTheme.typography.headlineSmall.toSpanStyle()) {
                    append(rxUnitText)
                }
            }

            Text(
                modifier = Modifier,
                text = formattedStat,
            )
        }
    }
}

@Composable
private fun TrafficGraphCard(
    trafficMetrics: List<TrafficMetric>,
    rxLineColor: Color,
    modifier: Modifier = Modifier,
) {
    Graph(
        data = trafficMetrics.mapIndexed { index, metric ->
            GraphEntry(index.toFloat(), metric.rxBytesPerSec)
        },
        lineColor = rxLineColor,
        maxDataPoints = TRAFFIC_METRICS_WINDOW_SECS,
        unitScaleY = 128,
        getY = { if (it > trafficMetrics.lastIndex) 0f else trafficMetrics[it].rxBytesPerSec },
        getYTickLabel = { bytes ->
            getBytesString(bytes).run {
                "${formatBigDecimal(first)}$second"
            }
        },
        modifier = modifier,
    )
}


private fun getBytesString(bytes: Float): Pair<BigDecimal, String> {
    val bigDecimalValue = bytes.toBigDecimal()
    val unitString = when {
        bigDecimalValue >= TB_SIZE -> "TB"
        bigDecimalValue >= GB_SIZE -> "GB"
        bigDecimalValue >= MB_SIZE -> "MB"
        bigDecimalValue >= KB_SIZE -> "KB"
        else -> "B"
    }
    val unitValue = when (unitString) {
        "B" -> bigDecimalValue
        "KB" -> bigDecimalValue.divide(KB_SIZE, 2, RoundingMode.HALF_UP)
        "MB" -> bigDecimalValue.divide(MB_SIZE, 2, RoundingMode.HALF_UP)
        "GB" -> bigDecimalValue.divide(GB_SIZE, 2, RoundingMode.HALF_UP)
        else -> bigDecimalValue.divide(TB_SIZE, 2, RoundingMode.HALF_UP)
    }
    return Pair(unitValue, unitString)
}


private fun formatBigDecimal(number: BigDecimal): String {
    val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US)).apply {
        isGroupingUsed = false
    }
    return decimalFormat.format(number)
}

private val KB_SIZE = 1024.toBigDecimal()
private val MB_SIZE = 1_048_576.toBigDecimal()
private val GB_SIZE = 1_073_741_824.toBigDecimal()
private val TB_SIZE = 1_099_511_627_776.toBigDecimal()
