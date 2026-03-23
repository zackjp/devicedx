package com.zackjp.devicedx.shared.ui

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer



@Composable
fun rememberIsInPipMode(
    isAllowedProvider: () -> Boolean,
    aspectRatioNumerator: Int,
    aspectRatioDenominator: Int,
): Boolean {
    val activity = LocalContext.current.findActivity()
    val isAllowed = isAllowedProvider()
    var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }

    DisposableEffect(
        activity,
        isAllowed,
        aspectRatioNumerator,
        aspectRatioDenominator,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val params = PictureInPictureParams.Builder()
                .setAutoEnterEnabled(isAllowed)
                .setAspectRatio(Rational(aspectRatioNumerator, aspectRatioDenominator))
                .build()
            activity.setPictureInPictureParams(params)
        }

        val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
            pipMode = info.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(listener)

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val params = PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(false)
                    .build()
                activity.setPictureInPictureParams(params)
            }
            activity.removeOnPictureInPictureModeChangedListener(listener)
        }
    }

    return pipMode
}

private fun Context.findActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Context was expected to have an Activity")
}