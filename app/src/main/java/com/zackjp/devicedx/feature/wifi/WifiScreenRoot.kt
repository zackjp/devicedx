package com.zackjp.devicedx.feature.wifi

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
import com.zackjp.devicedx.system.WifiInfo
import kotlinx.coroutines.launch

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

        WifiDataGrid(
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
private fun WifiDataGrid(
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

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(128.dp),
        userScrollEnabled = false,
    ) {
        items(stats) { stat ->
            WifiInfoStat(
                modifier = Modifier
                    .height(96.dp)
                    .padding(4.dp),
                stat = stat,
            )
        }
    }
}

@Composable
private fun WifiInfoStat(
    modifier: Modifier = Modifier,
    stat: Pair<String, String>
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                text = stat.first,
            )

            val style = MaterialTheme.typography.headlineSmall
            BasicText(
                autoSize = TextAutoSize.StepBased(maxFontSize = style.fontSize),
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                style = style,
                text = stat.second,
            )
        }
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
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            StartMonitorButton(
                isMonitorActive = isMonitorActive,
                modifier = Modifier.padding(horizontal = 16.dp),
                onStartMonitor = onStartMonitor,
                onStopMonitor = onStopMonitor,
                permissionStatus = permissionStatus,
            )
            Spacer(Modifier.height(8.dp))
        }

        wifiScanResults(
            rowModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            wifiNames = wifiNames,
        )
    }
}

@Composable
private fun StartMonitorButton(
    isMonitorActive: Boolean,
    modifier: Modifier = Modifier,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit,
    permissionStatus: PermissionStatus,
) {
    val context = LocalContext.current
    val (textResId, onClick) = if (permissionStatus == PermissionStatus.DeniedPermanently) {
        val launchSettingsAction = {
            val launchSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(launchSettingsIntent)
        }
        R.string.wifi_open_settings to launchSettingsAction
    } else if (isMonitorActive) {
        R.string.wifi_stop_monitor to onStopMonitor
    } else {
        R.string.wifi_start_monitor to onStartMonitor
    }

    Column(modifier) {
        Button(onClick = onClick) {
            Text(stringResource(textResId))
        }

        if (permissionStatus == PermissionStatus.DeniedTemporarily) {
            Text(stringResource(R.string.wifi_fine_location_permission_rationale))
        } else if (permissionStatus == PermissionStatus.DeniedPermanently) {
            Text(stringResource(R.string.wifi_fine_location_permission_denied))
        }
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
