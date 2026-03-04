package com.zackjp.devicedx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = DarkSlate,
    secondary = NeonGreen,
    outlineVariant = OffWhite,
    onBackground = OffWhite,
    onSurface = OffWhite,
    onSurfaceVariant = OffWhite,
)

@Composable
fun DeviceDxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}