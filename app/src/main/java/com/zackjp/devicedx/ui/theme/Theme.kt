package com.zackjp.devicedx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = IrisLight,
    onPrimary = Platinum,
    secondary = Turquoise,
    onSecondary = Ink,
    outlineVariant = Iris,
    background = NightShade,
    onBackground = Platinum,
    surface = NightShade,
    onSurface = Platinum,
    surfaceContainerHighest = DarkSlate,
    surfaceVariant = DarkSlate,
    onSurfaceVariant = Platinum,
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