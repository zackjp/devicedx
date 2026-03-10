package com.zackjp.devicedx.feature.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zackjp.devicedx.R
import com.zackjp.devicedx.navigation.NavActions
import com.zackjp.devicedx.shared.ui.AppCard
import com.zackjp.devicedx.shared.ui.PrimaryButton
import com.zackjp.devicedx.ui.theme.Turquoise


@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    navActions: NavActions,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    Surface(modifier) {
        val cardInfoList = rememberDashboardCards(navActions)

        Box(
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(cardInfoList) { cardInfo ->
                    DashboardCard(
                        cardInfo = cardInfo,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    cardInfo: DashCardInfo,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            CardDescriptionRow(
                modifier = Modifier.fillMaxWidth(),
                cardInfo = cardInfo,
            )

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = cardInfo.navAction,
                text = stringResource(cardInfo.launchTextId),
            )
        }
    }
}

@Composable
private fun CardDescriptionRow(
    modifier: Modifier = Modifier,
    cardInfo: DashCardInfo,
) {
    Row(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .aspectRatio(1f)
                .drawWithCache {
                    val bg = cardInfo.cardColorTheme.copy(alpha = 0.15f)
                    val cornerRadius = CornerRadius(10.dp.toPx())
                    onDrawBehind {
                        drawRoundRect(
                            color = bg,
                            cornerRadius = cornerRadius,
                        )
                    }
                }
                .padding(12.dp)
        ) {
            Icon(
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                painter = painterResource(cardInfo.iconResId),
                tint = cardInfo.cardColorTheme,
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                text = stringResource(cardInfo.titleTextId),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth(),
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                text = stringResource(cardInfo.descriptionTextId),
            )
        }
    }
}

@Composable
private fun rememberDashboardCards(
    navActions: NavActions,
): List<DashCardInfo> = remember(navActions) {
    listOf(
        DashCardInfo(
            titleTextId = R.string.dashboard_wifi_title,
            descriptionTextId = R.string.dashboard_wifi_description,
            launchTextId = R.string.dashboard_wifi_open_monitor,
            iconResId = R.drawable.ic_rounded_android_wifi_3_bar_24,
            cardColorTheme = Turquoise,
            navAction = navActions.toWifiMonitor,
        ),
        DashCardInfo(
            titleTextId = R.string.dashboard_latency_title,
            descriptionTextId = R.string.dashboard_latency_description,
            launchTextId = R.string.dashboard_latency_open_monitor,
            iconResId = R.drawable.ic_rounded_multiple_stop_24,
            cardColorTheme = Turquoise,
            navAction = navActions.toLatencyMonitor,
        ),
        DashCardInfo(
            titleTextId = R.string.dashboard_traffic_title,
            descriptionTextId = R.string.dashboard_traffic_description,
            launchTextId = R.string.dashboard_traffic_open_monitor,
            iconResId = R.drawable.ic_rounded_traffic_24,
            cardColorTheme = Turquoise,
            navAction = navActions.toTrafficMonitor,
        ),
    )
}

private data class DashCardInfo(
    @param:StringRes val titleTextId: Int,
    @param:StringRes val descriptionTextId: Int,
    @param:StringRes val launchTextId: Int,
    @param:DrawableRes val iconResId: Int,
    val cardColorTheme: Color,
    val navAction: () -> Unit,
)
