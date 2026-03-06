package com.zackjp.devicedx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.zackjp.devicedx.navigation.DeviceDxNav3Graph
import com.zackjp.devicedx.ui.theme.DeviceDxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeviceDxTheme {
                var isInPipMode by remember { mutableStateOf(isInPictureInPictureMode) }
                DisposableEffect(Unit) {
                    val listener: Consumer<PictureInPictureModeChangedInfo> = { info ->
                        isInPipMode = info.isInPictureInPictureMode
                    }
                    addOnPictureInPictureModeChangedListener(listener)
                    onDispose { removeOnPictureInPictureModeChangedListener(listener) }
                }

                Scaffold(
                    modifier = Modifier.fillMaxWidth(),
                    topBar = {
                        if (!isInPipMode) {
                            TopAppBar(
                                title = { Text(getString(R.string.app_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TopAppBarDefaults.topAppBarColors().copy(
                                    containerColor = Color.Transparent,
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    DeviceDxNav3Graph(
                        innerPadding = innerPadding,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
