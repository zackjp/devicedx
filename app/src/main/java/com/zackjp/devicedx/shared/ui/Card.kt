package com.zackjp.devicedx.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.zackjp.devicedx.ui.theme.DarkSlate
import com.zackjp.devicedx.ui.theme.IrisLight


private val AppCardBorderGradient = Brush.linearGradient(
    0.0f to IrisLight.copy(alpha = 0.15f),
    1f to IrisLight.copy(alpha = 0.4f),
    start = Offset.Infinite.copy(x = 0f),
    end = Offset.Infinite.copy(y = 0f),
)

private val AppCardBorderStroke = BorderStroke(
    width = 1.dp,
    brush = AppCardBorderGradient,
)


@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        border = AppCardBorderStroke,
        colors = CardDefaults.cardColors(
            containerColor = DarkSlate,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier,
        ) {
            content()
        }
    }
}
