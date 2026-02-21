package com.zackjp.devicedx.feature.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zackjp.devicedx.R
import com.zackjp.devicedx.navigation.NavActions


@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    navActions: NavActions,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    Surface(modifier) {
        val buttonInfoList = remember(navActions) {
            listOf(
                DashButtonInfo(
                    textResId = R.string.dashboard_open_wifi_monitor,
                    iconResId = R.drawable.ic_rounded_android_wifi_3_bar_24,
                    navAction = navActions.toWifiMonitor,
                ),
                DashButtonInfo(
                    textResId = R.string.dashboard_open_latency_monitor,
                    iconResId = R.drawable.ic_rounded_multiple_stop_24,
                    navAction = navActions.toLatencyMonitor,
                ),
                DashButtonInfo(
                    textResId = R.string.dashboard_open_traffic_monitor,
                    iconResId = R.drawable.ic_rounded_traffic_24,
                    navAction = navActions.toTrafficMonitor,
                ),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(buttonInfoList) { buttonInfo ->
                DashboardButton(
                    buttonInfo = buttonInfo,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardButton(
    buttonInfo: DashButtonInfo,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = buttonInfo.navAction,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                painter = painterResource(buttonInfo.iconResId)
            )
            Text(
                modifier = Modifier,
                text = stringResource(buttonInfo.textResId),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class DashButtonInfo(
    @param:StringRes val textResId: Int,
    @param:DrawableRes val iconResId: Int,
    val navAction: () -> Unit,
)