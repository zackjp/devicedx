package com.zackjp.devicedx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SoftCyan,
    onPrimary = DarkSlate,
    secondary = SoftIndigo,
    outlineVariant = OffWhite,
    onBackground = OffWhite,
    onSurface = OffWhite,
    surfaceVariant = DarkSlate,
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