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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R
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
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val pagerState = rememberPagerState { 2 }
            val tabs = listOf(
                stringResource(R.string.wifi_tab_stats),
                stringResource(R.string.wifi_tab_scan),
            )
            val coroutineScope = rememberCoroutineScope()

            PrimaryTabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = pagerState.currentPage,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == pagerState.currentPage,
                        onClick = { coroutineScope.launch { pagerState.scrollToPage(index) } },
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text(tab) },
                    )
                }
            }

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
                            wifiStrength = state.wifiStrength,
                        )
                    }

                    1 -> {
                        WifiScanPage(
                            isMonitorActive = state.isMonitorActive,
                            modifier = Modifier.fillMaxSize(),
                            onStartMonitor = viewModel::startMonitor,
                            onStopMonitor = viewModel::stopMonitor,
                            permissionStatus = state.permissionStatus,
                            wifiNames = state.wifiNames
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiStatsPage(
    modifier: Modifier = Modifier,
    wifiStrength: Int,
) {
    Column(
        modifier = modifier,
    ) {
        WifiStrength(
            modifier = Modifier
                .widthIn(128.dp, 196.dp)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally),
            wifiStrength,
        )
    }
}

@Composable
fun WifiStrength(
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
