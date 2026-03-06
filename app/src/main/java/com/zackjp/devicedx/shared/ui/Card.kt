package com.zackjp.devicedx.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


private val GlassBorderGradient = Brush.linearGradient(
    0.0f to Color.White.copy(alpha = 0.15f),
    1f to Color.White.copy(alpha = 0.5f),
)

private val GlassBackgroundGradient = Brush.linearGradient(
    0.0f to Color.White.copy(alpha = 0.1f),
    1f to Color.White.copy(alpha = 0.2f),
    start = Offset.Infinite.copy(y = 0f),
    end = Offset.Infinite.copy(x = 0f),
)


@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        border = BorderStroke(
            1.dp,
            GlassBorderGradient,
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .drawBehind {
                    drawRect(
                        GlassBackgroundGradient,
                    )
                },
        ) {
            content()
        }
    }
}
