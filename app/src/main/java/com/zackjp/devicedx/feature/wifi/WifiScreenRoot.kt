package com.zackjp.devicedx.feature.wifi

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.shared.ui.AppCard
import com.zackjp.devicedx.shared.ui.ScreenScaffold
import com.zackjp.devicedx.system.WifiInfo
import com.zackjp.devicedx.ui.theme.MediumGray
import kotlinx.coroutines.launch


private val LightBlueLink = Color(0xFF4BB2F9)

private val TAB_TITLES = listOf(
    R.string.wifi_tab_stats,
    R.string.wifi_tab_scan,
)


private val WIFI_QUALITY_LEVELS = listOf(
    WifiQuality(
        qualityStringRes = R.string.wifi_connection_quality_poor,
        qualityDrawableRes = R.drawable.ic_rounded_wifi_1_bar_24,
    ),
    WifiQuality(
        qualityStringRes = R.string.wifi_connection_quality_fair,
        qualityDrawableRes = R.drawable.ic_rounded_wifi_2_bar_24,
    ),
    WifiQuality(
        qualityStringRes = R.string.wifi_connection_quality_good,
        qualityDrawableRes = R.drawable.ic_rounded_android_wifi_3_bar_24,
    ),
    WifiQuality(
        qualityStringRes = R.string.wifi_connection_quality_excellent,
        qualityDrawableRes = R.drawable.ic_rounded_android_wifi_4_bar_24,
    ),
)

@Composable
fun WifiScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: WifiViewModel = hiltViewModel()
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val localActivity = LocalActivity.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val shouldShowRationale = localActivity?.let { activity ->
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    ACCESS_FINE_LOCATION,
                )
            } ?: false

            viewModel.onFineLocationPermissionResult(isGranted, shouldShowRationale)
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            if (it is WifiScreenEvent.LaunchFineLocation) {
                launcher.launch(ACCESS_FINE_LOCATION)
            }
        }
    }

    ScreenScaffold(
        modifier = modifier,
    ) {
        ReadyContent(
            modifier = Modifier.fillMaxWidth(),
            onStartMonitor = viewModel::startMonitor,
            onStopMonitor = viewModel::stopMonitor,
            state = state,
        )
    }
}

@Composable
private fun ReadyContent(
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit,
    state: WifiScreenState,
) {
    Column(
        modifier = modifier,
    ) {
        val pagerState = rememberPagerState { TAB_TITLES.size }
        val tabNames = TAB_TITLES.map { stringResource(it) }

        TabRow(
            pagerState = pagerState,
            tabNames = tabNames,
        )
        PagerContent(
            pagerState = pagerState,
            screenState = state,
            onStartMonitor = onStartMonitor,
            onStopMonitor = onStopMonitor,
        )
    }
}

@Composable
private fun TabRow(
    pagerState: PagerState,
    tabNames: List<String>
) {
    val coroutineScope = rememberCoroutineScope()
    PrimaryTabRow(
        modifier = Modifier.fillMaxWidth(),
        selectedTabIndex = pagerState.currentPage,
    ) {
        tabNames.forEachIndexed { index, tab ->
            Tab(
                selected = index == pagerState.currentPage,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier.fillMaxWidth(),
                text = { Text(tab) },
            )
        }
    }
}

@Composable
private fun PagerContent(
    pagerState: PagerState,
    screenState: WifiScreenState,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit
) {
    HorizontalPager(
        modifier = Modifier.fillMaxWidth(),
        state = pagerState,
        verticalAlignment = Alignment.Top,
    ) { pageIndex ->
        when (pageIndex) {
            0 -> {
                WifiStatsPage(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    wifiInfo = screenState.wifiInfo,
                )
            }

            1 -> {
                WifiScanPage(
                    modifier = Modifier.fillMaxSize(),
                    onStartMonitor = onStartMonitor,
                    onStopMonitor = onStopMonitor,
                    permissionStatus = screenState.permissionStatus,
                    wifiNames = screenState.wifiNames
                )
            }
        }
    }
}

@Composable
private fun WifiStatsPage(
    modifier: Modifier = Modifier,
    wifiInfo: WifiInfo,
) {
    Column(
        modifier = modifier,
    ) {
        WifiStrengthCard(
            modifier = Modifier
                .fillMaxWidth(),
            wifiRssi = wifiInfo.rssi,
            wifiPercent = wifiInfo.wifiStrengthPercent,
        )

        Spacer(modifier = Modifier.height(16.dp))

        ConnectionDetailsCard(
            modifier = Modifier.fillMaxWidth(),
            wifiInfo = wifiInfo,
        )
    }
}

@Composable
private fun WifiStrengthCard(
    modifier: Modifier,
    wifiRssi: Int,
    wifiPercent: Float,
) {
    AppCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = stringResource(R.string.wifi_title_signal_strength),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.displayMedium,
                    text = wifiRssi.toString(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    style = MaterialTheme.typography.displaySmall,
                    text = stringResource(R.string.wifi_unit_decibel_milliwatts),
                )
            }

            SignalQualityIndicator(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                signalPercent = wifiPercent,
            )

        }
    }
}

@Composable
fun SignalQualityIndicator(
    modifier: Modifier = Modifier,
    shape: Shape,
    signalPercent: Float,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = modifier.background(
                color = MaterialTheme.colorScheme.surface,
                shape = shape
            )
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(signalPercent)
                    .height(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape,
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            WIFI_QUALITY_LEVELS.forEach {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(it.qualityStringRes),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ConnectionDetailsCard(
    modifier: Modifier = Modifier,
    wifiInfo: WifiInfo,
) {
    val connectionQualityInfo = connectionQualityInfo(wifiInfo.wifiStrength)
    val connectionQualityText = stringResource(connectionQualityInfo.qualityStringRes)

    val stats = listOf(
        stringResource(R.string.wifi_info_grid_label_network) to wifiInfo.ssid,
        stringResource(R.string.wifi_info_grid_label_connection_quality) to connectionQualityText,
        stringResource(R.string.wifi_info_grid_label_ip_address) to wifiInfo.ipAddress,
        stringResource(R.string.wifi_info_grid_label_link_speed) to "${wifiInfo.linkSpeedMbps} Mbps",
    )

    AppCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.wifi_current_connection),
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(12.dp))


            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stats.forEach { stat ->
                    ConnectionDetailStat(
                        modifier = Modifier.weight(1f),
                        stat = stat,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionDetailStat(
    modifier: Modifier = Modifier,
    stat: Pair<String, String>,
) {
    val (statLabel, statValue) = stat

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

@Composable
private fun WifiScanPage(
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit,
    permissionStatus: PermissionStatus,
    wifiNames: List<String>,
) {
    var scanResultsExpanded by remember { mutableStateOf(false) }
    val networkBackgroundColor = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            CollapsibleButton(
                isExpanded = scanResultsExpanded,
                modifier = Modifier
                    .clickable {
                        scanResultsExpanded = !scanResultsExpanded
                        if (scanResultsExpanded) {
                            onStartMonitor()
                        } else {
                            onStopMonitor()
                        }
                    }
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.wifi_show_networks),
            )
            Spacer(Modifier.height(8.dp))
        }

        if (scanResultsExpanded) {
            when (permissionStatus) {
                PermissionStatus.DeniedTemporarily -> {
                    scanPermissionRationaleMessage(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                PermissionStatus.DeniedPermanently -> {
                    scanPermissionDeniedMessage(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                else -> {
                    wifiScanResults(
                        rowModifier = Modifier
                            .fillMaxWidth(),
                        wifiNames = wifiNames,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleButton(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    text: String,
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            animatable.animateTo(1f)
        } else {
            animatable.animateTo(0f)
        }
    }

    Row(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier.graphicsLayer {
                rotationZ = 90f * animatable.value
            },
            text = ">",
        )
        Text(" $text")
    }
}

private fun LazyListScope.scanPermissionRationaleMessage(
    modifier: Modifier = Modifier,
) {
    item {
        Text(
            modifier = modifier,
            text = stringResource(R.string.wifi_fine_location_permission_rationale),
        )
    }
}

private fun LazyListScope.scanPermissionDeniedMessage(
    modifier: Modifier = Modifier,
) {
    item {
        val context = LocalContext.current
        val launchSettingsAction = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                this.data = Uri.fromParts("package", context.packageName, null)
            })
        }

        val stylizedText = buildAnnotatedString {
            val rawString = stringResource(R.string.wifi_fine_location_permission_denied)
            append(
                AnnotatedString.fromHtml(
                    htmlString = rawString,
                    linkStyles = TextLinkStyles(style = SpanStyle(color = LightBlueLink)),
                    linkInteractionListener = { linkAnnotation ->
                        if (linkAnnotation is LinkAnnotation.Url) {
                            if (linkAnnotation.url == "#app-permissions") {
                                launchSettingsAction()
                            }
                        }
                    }
                )
            )
        }

        Text(
            modifier = modifier,
            text = stylizedText,
        )
    }
}

private fun connectionQualityInfo(wifiStrength: Int): WifiQuality {
    val adjustedIndex = (wifiStrength - 1).coerceIn(0, WIFI_QUALITY_LEVELS.lastIndex)
    return WIFI_QUALITY_LEVELS[adjustedIndex]
}

private fun LazyListScope.wifiScanResults(
    rowModifier: Modifier = Modifier,
    wifiNames: List<String>,
) {
    items(wifiNames) { wifiName ->
        Card(
            modifier = rowModifier,
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    modifier = rowModifier,
                    text = wifiName,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private data class WifiQuality(
    @param:StringRes val qualityStringRes: Int,
    @param:DrawableRes val qualityDrawableRes: Int
)