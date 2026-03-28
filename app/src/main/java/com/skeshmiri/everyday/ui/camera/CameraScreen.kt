package com.skeshmiri.everyday.ui.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skeshmiri.everyday.camera.CameraController
import com.skeshmiri.everyday.ui.common.OnResume
import com.skeshmiri.everyday.ui.common.ScreenHeader
import com.skeshmiri.everyday.ui.common.UriImage

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    cameraController: CameraController,
    onOpenGallery: () -> Unit,
    onOpenReview: (dateKey: String, tempPath: String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onPermissionResult,
    )

    OnResume {
        viewModel.syncPermission(context)
        viewModel.refresh()
    }

    LaunchedEffect(uiState.hasCameraPermission, uiState.todayPhoto, previewView, lifecycleOwner) {
        val boundPreview = previewView
        if (uiState.hasCameraPermission && uiState.todayPhoto == null && boundPreview != null) {
            runCatching { cameraController.bind(lifecycleOwner, boundPreview) }
        } else {
            cameraController.unbind()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.unbind()
        }
    }

    CameraScreenContent(
        uiState = uiState,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onOpenGallery = onOpenGallery,
        onCapture = {
            viewModel.capture(cameraController) { dateKey, tempFile ->
                onOpenReview(dateKey, tempFile.absolutePath)
            }
        },
        preview = {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewContext ->
                    PreviewView(previewContext).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this
                    }
                },
                update = { previewView = it },
            )
        },
    )
}

@Composable
fun CameraScreenContent(
    uiState: CameraUiState,
    onRequestPermission: () -> Unit,
    onOpenGallery: () -> Unit,
    onCapture: () -> Unit,
    preview: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            ScreenHeader(
                title = "Everyday",
                actions = {
                    IconButton(onClick = onOpenGallery) {
                        Icon(Icons.Rounded.Collections, contentDescription = "Open gallery")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            !uiState.hasCameraPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "The camera stays local to this app. Grant access to take your daily selfie.",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = uiState.errorMessage ?: "No network, no account, just one photo a day.",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.padding(top = 24.dp),
                    ) {
                        Text("Allow camera")
                    }
                    TextButton(
                        onClick = onOpenGallery,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Open gallery")
                    }
                }
            }

            uiState.todayPhoto != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        text = "Today's photo is already saved.",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        UriImage(
                            uri = uiState.todayPhoto.uri,
                            contentDescription = "Today's selfie",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        text = "Come back tomorrow for the next one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onOpenGallery,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open gallery")
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.Black),
                ) {
                    preview()

                    if (uiState.errorMessage != null) {
                        ElevatedCard(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp),
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledIconButton(
                            onClick = onCapture,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape),
                            enabled = !uiState.isCapturing,
                        ) {
                            if (uiState.isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = "Take photo",
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
