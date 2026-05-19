package com.zackjp.devicedx.feature.traffic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zackjp.devicedx.shared.ui.ScreenScaffold


@Composable
fun TrafficHistoryScreenRoot(
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text("Coming soon.")
        }
    }
}