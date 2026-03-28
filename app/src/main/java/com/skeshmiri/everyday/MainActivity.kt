package com.skeshmiri.everyday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.skeshmiri.everyday.di.AppContainer
import com.skeshmiri.everyday.ui.EverydayApp
import com.skeshmiri.everyday.ui.theme.EverydayTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }
    private val cameraController by lazy { appContainer.createCameraController() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainer.tempPhotoStore.cleanupStaleFiles()

        setContent {
            EverydayTheme {
                EverydayApp(
                    container = appContainer,
                    cameraController = cameraController,
                )
            }
        }
    }

    override fun onDestroy() {
        cameraController.close()
        super.onDestroy()
    }
}

