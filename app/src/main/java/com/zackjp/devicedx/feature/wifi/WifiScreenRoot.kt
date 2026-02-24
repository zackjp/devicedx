package com.zackjp.devicedx.feature.wifi

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
        LazyColumn {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WifiStrength(
                        modifier = Modifier
                            .widthIn(128.dp, 196.dp)
                            .aspectRatio(1f),
                        state.wifiStrength,
                    )
                }
            }

            item {
                val context = LocalContext.current

                val (textResId, onClick) = when {
                    state.permissionStatus != PermissionStatus.DeniedPermanently -> {
                        when (state.isMonitorActive) {
                            true -> R.string.wifi_stop_monitor to { viewModel.stopMonitor() }
                            false -> R.string.wifi_start_monitor to { viewModel.startMonitor() }
                        }
                    }

                    else -> {
                        R.string.wifi_open_settings to {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    }
                }

                Button(onClick = onClick) {
                    Text(stringResource(textResId))
                }

                when (state.permissionStatus) {
                    PermissionStatus.DeniedTemporarily -> Text(stringResource(R.string.wifi_fine_location_permission_rationale))
                    PermissionStatus.DeniedPermanently -> Text(stringResource(R.string.wifi_fine_location_permission_denied))
                    else -> {}
                }
            }

            wifiScanResults(state.wifiNames)
        }
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

private fun LazyListScope.wifiScanResults(
    wifiNames: List<String>,
) {
    items(wifiNames) { wifiName ->
        Spacer(Modifier.height(16.dp))
        Text(wifiName)
    }
}
