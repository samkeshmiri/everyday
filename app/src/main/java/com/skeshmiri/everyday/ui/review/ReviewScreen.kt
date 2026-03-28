package com.skeshmiri.everyday.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skeshmiri.everyday.ui.common.FourThreePortraitFrame
import com.skeshmiri.everyday.ui.common.ScreenHeader
import com.skeshmiri.everyday.ui.common.UriImage

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onSaved: () -> Unit,
    onRetake: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val frameHeight = screenHeight * 0.5f
    BackHandler(onBack = { viewModel.discard(onRetake) })

    Scaffold(
        topBar = {
            ScreenHeader(title = "Review")
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            val tempUri = viewModel.tempUri
            if (tempUri != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    FourThreePortraitFrame(
                        modifier = Modifier.height(frameHeight),
                    ) {
                        UriImage(
                            uri = tempUri,
                            contentDescription = "Review captured photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.errorMessage ?: "No photo to review.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = { viewModel.save { onSaved() } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.tempFile != null && !uiState.isSaving,
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    } else {
                        Text("Save")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.discard(onRetake) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                ) {
                    Text("Retake")
                }
            }
        }
    }
}
