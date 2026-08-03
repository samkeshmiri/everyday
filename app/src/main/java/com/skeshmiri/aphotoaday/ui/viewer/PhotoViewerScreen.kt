package com.skeshmiri.aphotoaday.ui.viewer

import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.skeshmiri.aphotoaday.ui.common.ScreenHeader
import com.skeshmiri.aphotoaday.ui.common.UriImage
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

const val PhotoViewerImageTag = "photo_viewer_image"

data class PhotoViewerItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val contentDescription: String,
    val capturedAt: Instant?,
)

@Composable
fun PhotoViewerScreen(
    uri: Uri,
    title: String,
    contentDescription: String,
    capturedAt: Instant?,
    onClose: () -> Unit,
    onOpenInGallery: () -> Unit,
    onShare: () -> Unit,
) {
    PhotoViewerScreen(
        photos = listOf(
            PhotoViewerItem(
                id = uri.toString().hashCode().toLong(),
                uri = uri,
                title = title,
                contentDescription = contentDescription,
                capturedAt = capturedAt,
            ),
        ),
        initialPhotoId = uri.toString().hashCode().toLong(),
        onClose = onClose,
        onOpenInGallery = { onOpenInGallery() },
        onShare = { onShare() },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    photos: List<PhotoViewerItem>,
    initialPhotoId: Long,
    onClose: () -> Unit,
    onOpenInGallery: (PhotoViewerItem) -> Unit,
    onShare: (PhotoViewerItem) -> Unit,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    if (photos.isEmpty()) {
        Scaffold(
            topBar = {
                ScreenHeader(title = "Photo")
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClose,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    var selectedPhotoId by rememberSaveable(initialPhotoId) { mutableLongStateOf(initialPhotoId) }
    val photoIds = remember(photos) { photos.map { it.id } }
    val initialPage = photoIds.indexOf(initialPhotoId).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { photos.size }
    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.getOrNull(initialPage)
    val headerTitle = currentPhoto
        ?.let { photo ->
            photo.capturedAt
                ?.let {
                    "${formatPhotoDateTitle(photo.title)}, ${
                        DateFormat.getTimeFormat(context).format(Date.from(it))
                    }"
                }
                ?: formatPhotoDateTitle(photo.title)
        }
        ?: "Photo"

    LaunchedEffect(photoIds, initialPhotoId) {
        val targetId = selectedPhotoId
            .takeIf { it in photoIds }
            ?: initialPhotoId.takeIf { it in photoIds }
        val targetPage = targetId?.let(photoIds::indexOf) ?: -1
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        photos.getOrNull(pagerState.currentPage)?.let { selectedPhotoId = it.id }
    }

    Scaffold(
        topBar = {
            ScreenHeader(title = headerTitle)
        },
        floatingActionButton = {
            currentPhoto?.let { photo ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    FloatingActionButton(
                        onClick = { onOpenInGallery(photo) },
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = "Open in gallery",
                        )
                    }
                    FloatingActionButton(
                        onClick = { onShare(photo) },
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share photo",
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(PhotoViewerImageTag)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClose,
                ),
            key = { page -> photos[page].id },
        ) { page ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val maxBitmapDimensionPx = with(density) {
                    maxOf(maxWidth, maxHeight).roundToPx()
                }
                val photo = photos[page]
                UriImage(
                    uri = photo.uri,
                    contentDescription = photo.contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    maxBitmapDimensionPx = maxBitmapDimensionPx,
                )
            }
        }
    }
}

private fun formatPhotoDateTitle(dateKey: String): String {
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return dateKey
    val month = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH).format(date)
    return "$month ${date.dayOfMonth}${date.dayOfMonth.ordinalSuffix()} ${date.year}"
}

private fun Int.ordinalSuffix(): String =
    if (this % 100 in 11..13) {
        "th"
    } else {
        when (this % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
