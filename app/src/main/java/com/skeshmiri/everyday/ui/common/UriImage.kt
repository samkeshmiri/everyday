package com.skeshmiri.everyday.ui.common

import android.content.ContentResolver
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun UriImage(
    uri: Uri,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailSize: Dp? = null,
) {
    val context = LocalContext.current
    val thumbnailSizePx = thumbnailSize?.value?.toInt()
    val imageState by produceState<UriImageState>(initialValue = UriImageState.Loading, uri, thumbnailSizePx) {
        value = loadBitmap(context, uri, thumbnailSizePx)
            ?.let(UriImageState::Loaded)
            ?: UriImageState.Error
    }

    when (val state = imageState) {
        UriImageState.Loading -> {
            Box(
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }

        UriImageState.Error -> {
            Box(
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ImageNotSupported,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        is UriImageState.Loaded -> {
            Image(
                bitmap = state.bitmap,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }
    }
}

private suspend fun loadBitmap(
    context: Context,
    uri: Uri,
    thumbnailSizePx: Int?,
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (thumbnailSizePx != null && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.loadThumbnail(
                uri,
                Size(thumbnailSizePx, thumbnailSizePx),
                null,
            ).asImageBitmap()
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }.asImageBitmap()
        }
    }.getOrNull()
}

private sealed interface UriImageState {
    data object Loading : UriImageState
    data object Error : UriImageState
    data class Loaded(val bitmap: ImageBitmap) : UriImageState
}
