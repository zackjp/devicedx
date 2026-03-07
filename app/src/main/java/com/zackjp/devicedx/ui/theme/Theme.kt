package com.zackjp.devicedx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Sage,
    onPrimary = Ink,
    secondary = OffWhite,
    onSecondary = Sage,
    outlineVariant = OffWhite,
    background = Ink,
    onBackground = OffWhite,
    surface = Ink,
    onSurface = OffWhite,
    surfaceContainerHighest = SlateGray,
    surfaceVariant = SlateGray,
    onSurfaceVariant = OffWhite,
)

@Composable
fun DeviceDxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}