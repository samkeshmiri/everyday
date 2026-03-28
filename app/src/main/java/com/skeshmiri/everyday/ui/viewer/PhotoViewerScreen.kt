package com.skeshmiri.everyday.ui.viewer

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.skeshmiri.everyday.ui.common.ScreenHeader
import com.skeshmiri.everyday.ui.common.UriImage

@Composable
fun PhotoViewerScreen(
    uri: Uri,
    displayName: String,
) {
    Scaffold(
        topBar = {
            ScreenHeader(title = displayName)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            UriImage(
                uri = uri,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
