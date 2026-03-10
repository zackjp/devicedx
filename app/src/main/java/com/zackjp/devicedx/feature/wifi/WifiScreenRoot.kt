package com.zackjp.devicedx.feature.wifi

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.zackjp.devicedx.system.WifiInfo
import kotlinx.coroutines.launch


private val LightBlueLink = Color(0xFF4BB2F9)


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

    Surface(modifier) {
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
        val pagerState = rememberPagerState { 2 }
        val tabNames = listOf(
            stringResource(R.string.wifi_tab_stats),
            stringResource(R.string.wifi_tab_scan),
        )

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
                    isMonitorActive = screenState.isMonitorActive,
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
        WifiStrengthIcon(
            modifier = Modifier
                .widthIn(128.dp, 196.dp)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally),
            wifiInfo.wifiStrength,
        )

        ConnectionDetailsCard(
            modifier = Modifier.fillMaxWidth(),
            wifiInfo = wifiInfo,
        )
    }
}

@Composable
private fun WifiStrengthIcon(
    modifier: Modifier,
    wifiStrength: Int,
) {
    val wifiIconId = when (wifiStrength) {
        2 -> R.drawable.ic_rounded_wifi_2_bar_24
        3 -> R.drawable.ic_rounded_android_wifi_3_bar_24
        4 -> R.drawable.ic_rounded_android_wifi_4_bar_24
        else -> R.drawable.ic_rounded_wifi_1_bar_24
    }

    Column(
        modifier = modifier,
    ) {
        Icon(
            contentDescription = null,
            modifier = Modifier
                .widthIn(128.dp, 196.dp)
                .aspectRatio(1f),
            painter = painterResource(wifiIconId),
        )
    }
}

@Composable
private fun ConnectionDetailsCard(
    modifier: Modifier = Modifier,
    wifiInfo: WifiInfo,
) {
    val connectionQuality = when {
        wifiInfo.wifiStrength >= 3 -> stringResource(R.string.wifi_connection_quality_excellent)
        wifiInfo.wifiStrength == 2 -> stringResource(R.string.wifi_connection_quality_good)
        wifiInfo.wifiStrength == 1 -> stringResource(R.string.wifi_connection_quality_fair)
        else -> stringResource(R.string.wifi_connection_quality_poor)
    }
    val stats = listOf(
        stringResource(R.string.wifi_info_grid_label_network) to wifiInfo.ssid,
        stringResource(R.string.wifi_info_grid_label_connection_quality) to connectionQuality,
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
                color = MaterialTheme.colorScheme.primary,
                text = "Current Connection",
                style = MaterialTheme.typography.titleMedium,
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            stats.forEach { stat ->
                ConnectionDetailStat(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    stat = stat,
                )
            }
        }
    }
}

@Composable
private fun ConnectionDetailStat(
    modifier: Modifier = Modifier,
    stat: Pair<String, String>,
) {
    val (statTitle, statInfo) = stat

    Row(
        modifier = modifier,
    ) {
        val dimmedColor = MaterialTheme.colorScheme.primary.copy(0.75f)
        Text(
            color = dimmedColor,
            maxLines = 1,
            text = statTitle,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            maxLines = 1,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.MiddleEllipsis,
            text = statInfo,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun WifiScanPage(
    isMonitorActive: Boolean,
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit,
    permissionStatus: PermissionStatus,
    wifiNames: List<String>,
) {
    var scanResultsExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                text = "Show Networks",
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
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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

private fun LazyListScope.wifiScanResults(
    rowModifier: Modifier = Modifier,
    wifiNames: List<String>,
) {
    items(wifiNames) { wifiName ->
        Text(
            modifier = rowModifier,
            text = wifiName,
        )
    }
}
