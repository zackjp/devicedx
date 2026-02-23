package com.zackjp.devicedx.feature.wifi

import android.Manifest.permission.ACCESS_FINE_LOCATION
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zackjp.devicedx.R

@Composable
fun WifiScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: WifiViewModel = hiltViewModel()
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.startMonitor()
            } else {
                viewModel.onFineLocationPermissionDenied()
            }
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
                val (textResId, onClick) = if (state.isMonitorActive) {
                    R.string.wifi_stop_monitor to { viewModel.stopMonitor() }
                } else {
                    R.string.wifi_start_monitor to { viewModel.startMonitor() }
                }

                Button(
                    onClick = onClick
                ) {
                    Text(stringResource(textResId))
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
