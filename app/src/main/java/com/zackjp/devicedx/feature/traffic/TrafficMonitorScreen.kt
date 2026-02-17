package com.zackjp.devicedx.feature.traffic

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zackjp.devicedx.R

@Composable
fun TrafficMonitorScreen(
    modifier: Modifier = Modifier,
) {
    Surface(modifier) {
        Text(stringResource(R.string.coming_soon_here))
    }
}
