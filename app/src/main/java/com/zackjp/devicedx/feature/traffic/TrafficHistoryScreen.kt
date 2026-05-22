package com.zackjp.devicedx.feature.traffic

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.model.TrafficSession
import com.zackjp.devicedx.shared.ui.ScreenScaffold
import com.zackjp.devicedx.ui.theme.CyberAmber
import com.zackjp.devicedx.ui.theme.Turquoise


private val RxLineColor = Turquoise
private val TxLineColor = CyberAmber

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
private fun ReadyContent(
    modifier: Modifier = Modifier,
    sessions: List<TrafficSession>
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(sessions, key = { it.id }) { session ->
            TrafficSessionRow(
                modifier = Modifier.fillMaxWidth(),
                session = session,
            )
        }
    }
}

@Composable
private fun TrafficSessionRow(
    modifier: Modifier = Modifier,
    session: TrafficSession,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.traffic_session_id_name, session.id),
            overflow = TextOverflow.MiddleEllipsis,
        )
        TrafficRowStat(
            modifier = Modifier.width(108.dp),
            iconId = R.drawable.ic_outline_arrow_downward_alt_24,
            tint = RxLineColor,
            text = session.totalRxBytes.toString(),
        )
        TrafficRowStat(
            modifier = Modifier.width(108.dp),
            iconId = R.drawable.ic_outline_arrow_upward_alt_24,
            tint = TxLineColor,
            text = session.totalTxBytes.toString(),
        )
    }
}

@Composable
private fun TrafficRowStat(
    modifier: Modifier = Modifier,
    @DrawableRes iconId: Int,
    tint: Color,
    text: String,
) {
    Row(
        modifier = modifier,
    ) {
        Icon(
            contentDescription = null,
            painter = painterResource(iconId),
            tint = tint,
        )
        Text(
            text = text,
        )
    }
}