package com.skeshmiri.everyday.ui.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skeshmiri.everyday.camera.CameraController
import com.skeshmiri.everyday.ui.common.FourThreePortraitFrame
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
    val density = LocalDensity.current
    val screenHeight = with(density) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }
    val frameHeight = screenHeight * 0.5f

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
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            text = "Today's photo is already saved.",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            FourThreePortraitFrame(
                                modifier = Modifier.height(frameHeight),
                            ) {
                                UriImage(
                                    uri = uiState.todayPhoto.uri,
                                    contentDescription = "Today's selfie",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
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
            }

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.Black),
                ) {
                    val topSectionHeight = frameHeight + 40.dp
                    val bottomSectionHeight = (maxHeight - topSectionHeight).coerceAtLeast(160.dp)
                    val captureButtonWidth = (maxWidth - 48.dp)
                        .coerceAtMost(320.dp)
                        .coerceAtLeast(220.dp)
                    val captureButtonHeight = (bottomSectionHeight * 0.4f)
                        .coerceAtMost(132.dp)
                        .coerceAtLeast(92.dp)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topSectionHeight)
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            FourThreePortraitFrame(
                                modifier = Modifier.height(frameHeight),
                            ) {
                                preview()
                            }

                            if (uiState.errorMessage != null) {
                                ElevatedCard(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp),
                                ) {
                                    Text(
                                        text = uiState.errorMessage,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = bottomSectionHeight)
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Button(
                                onClick = onCapture,
                                modifier = Modifier
                                    .width(captureButtonWidth)
                                    .height(captureButtonHeight),
                                enabled = !uiState.isCapturing,
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                if (uiState.isCapturing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = "Take photo",
                                        modifier = Modifier.size(captureButtonHeight * 0.3f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
