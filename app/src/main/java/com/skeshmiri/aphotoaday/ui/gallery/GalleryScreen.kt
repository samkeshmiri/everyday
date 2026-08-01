package com.skeshmiri.aphotoaday.ui.gallery

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skeshmiri.aphotoaday.export.ExportedGalleryVideo
import com.skeshmiri.aphotoaday.model.DailyPhoto
import com.skeshmiri.aphotoaday.ui.common.OnResume
import com.skeshmiri.aphotoaday.ui.common.UriImage
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenPhoto: (DailyPhoto) -> Unit,
    onOpenGuideSettings: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExportDialog by rememberSaveable { mutableStateOf(false) }

    OnResume(viewModel::refresh)

    val closeExportDialog = {
        showExportDialog = false
        viewModel.clearExportFeedback()
    }

    GalleryScreenContent(
        uiState = uiState,
        onOpenPhoto = onOpenPhoto,
        onOpenExportDialog = {
            showExportDialog = true
        },
        onOpenGuideSettings = onOpenGuideSettings,
    )

    if (showExportDialog) {
        GalleryExportDialog(
            uiState = uiState,
            onDismiss = closeExportDialog,
            onSelectFps = viewModel::selectFps,
            onExport = viewModel::exportVideo,
            onOpenExportedVideo = { exportedVideo ->
                openExportedVideo(context, exportedVideo)
            },
            onDone = closeExportDialog,
        )
    }
}

@Composable
internal fun GalleryScreenContent(
    uiState: GalleryUiState,
    onOpenPhoto: (DailyPhoto) -> Unit,
    onOpenExportDialog: () -> Unit,
    onOpenGuideSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (uiState.photos.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = onOpenExportDialog,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MovieCreation,
                            contentDescription = "Export video",
                        )
                    }
                }
                FloatingActionButton(
                    onClick = onOpenGuideSettings,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Guide settings",
                    )
                }
            }
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

            uiState.photos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = uiState.errorMessage ?: "No saved photos yet.",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            else -> {
                GalleryGrid(
                    photos = uiState.photos,
                    onOpenPhoto = onOpenPhoto,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun GalleryExportDialog(
    uiState: GalleryUiState,
    onDismiss: () -> Unit,
    onSelectFps: (Int) -> Unit,
    onExport: () -> Unit,
    onOpenExportedVideo: (ExportedGalleryVideo) -> Unit,
    onDone: () -> Unit,
) {
    val exportedVideo = uiState.exportedVideo
    val exportProgress = uiState.exportProgress
    val progressPercent = exportProgress?.fraction?.times(100)?.roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    uiState.isExporting -> "Exporting video"
                    exportedVideo == null -> "Export video"
                    else -> "Video saved"
                },
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (uiState.isExporting) {
                    Text("${uiState.photos.size} photos are being exported from oldest to newest.")
                    LinearProgressIndicator(
                        progress = {
                            exportProgress?.fraction ?: 0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = when {
                            exportProgress == null -> "Preparing export..."
                            else -> "${exportProgress.completedFrames} of ${exportProgress.totalFrames} photos exported" +
                                progressPercent?.let { " ($it%)" }.orEmpty()
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    exportTimeRemainingLabel(uiState)?.let { remainingLabel ->
                        Text(
                            text = remainingLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (exportedVideo == null) {
                    Text("${uiState.photos.size} photos will be exported from oldest to newest.")
                    Text(
                        text = "Estimated length: ${
                            GalleryVideoExportDefaults.formatDurationSeconds(uiState.estimatedDurationSeconds)
                        } seconds",
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GalleryVideoExportDefaults.fpsPresets.forEach { fps ->
                            FilterChip(
                                selected = uiState.selectedFps == fps,
                                onClick = { onSelectFps(fps) },
                                enabled = !uiState.isExporting,
                                label = { Text("$fps fps") },
                            )
                        }
                    }

                    uiState.exportErrorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text("Saved to ${exportedVideo.relativePath}")
                    Text("${exportedVideo.frameCount} photos at ${exportedVideo.fps} fps")
                    Text(
                        "Length: ${
                            GalleryVideoExportDefaults.formatDurationSeconds(exportedVideo.durationSeconds)
                        } seconds",
                    )
                }
            }
        },
        confirmButton = {
            if (uiState.isExporting) {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text("Close")
                }
            } else if (exportedVideo == null) {
                Button(
                    onClick = onExport,
                ) {
                    Text("Export MP4")
                }
            } else {
                Button(onClick = onDone) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (!uiState.isExporting && exportedVideo == null) {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text("Cancel")
                }
            } else if (exportedVideo != null) {
                TextButton(
                    onClick = { onOpenExportedVideo(exportedVideo) },
                ) {
                    Text("Open")
                }
            }
        },
    )
}

private fun openExportedVideo(context: Context, exportedVideo: ExportedGalleryVideo) {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(exportedVideo.uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, exportedVideo.displayName, exportedVideo.uri)
    }
    context.startActivity(viewIntent)
}

private fun exportTimeRemainingLabel(uiState: GalleryUiState): String? {
    val progress = uiState.exportProgress ?: return null
    val startedAtEpochMillis = uiState.exportStartedAtEpochMillis ?: return null
    if (progress.completedFrames <= 0 || progress.totalFrames <= 0) {
        return null
    }

    val fraction = progress.fraction
    if (fraction <= 0f || fraction >= 1f) {
        return null
    }

    val elapsedMillis = (System.currentTimeMillis() - startedAtEpochMillis).coerceAtLeast(0L)
    if (elapsedMillis <= 0L) {
        return null
    }

    val estimatedTotalMillis = (elapsedMillis / fraction).toLong()
    val remainingMillis = (estimatedTotalMillis - elapsedMillis).coerceAtLeast(0L)
    val remainingSeconds = remainingMillis / 1000.0
    return "About ${GalleryVideoExportDefaults.formatDurationSeconds(remainingSeconds)}s left"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryGrid(
    photos: List<DailyPhoto>,
    onOpenPhoto: (DailyPhoto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val monthSections = remember(photos) { photos.toMonthSections() }

    Box(modifier = modifier) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 2.dp,
                top = 8.dp,
                end = 10.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            monthSections.forEach { section ->
                item(
                    key = "month-${section.yearMonth}",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = section.progress,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(section.photos, key = { it.id }) { photo ->
                    Column(
                        modifier = Modifier
                            .animateItem()
                            .clickable { onOpenPhoto(photo) },
                    ) {
                        UriImage(
                            uri = photo.uri,
                            contentDescription = photo.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop,
                            thumbnailSize = 256.dp,
                        )
                    }
                }
            }
        }

        GalleryScrollbar(
            state = gridState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 12.dp, horizontal = 2.dp),
        )
    }
}

@Composable
private fun GalleryScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        val totalItems = state.layoutInfo.totalItemsCount
        val visibleItems = state.layoutInfo.visibleItemsInfo.size
        if (totalItems == 0 || visibleItems == 0 || totalItems <= visibleItems) {
            return@BoxWithConstraints
        }

        val scrollableItems = (totalItems - visibleItems).coerceAtLeast(1)
        val progress = (state.firstVisibleItemIndex.toFloat() / scrollableItems.toFloat())
            .coerceIn(0f, 1f)
        val thumbHeight = (maxHeight * (visibleItems.toFloat() / totalItems.toFloat()))
            .coerceAtLeast(32.dp)
            .coerceAtMost(maxHeight)
        val thumbOffset = (maxHeight - thumbHeight) * progress

        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
        Box(
            modifier = Modifier
                .padding(top = thumbOffset)
                .width(4.dp)
                .height(thumbHeight)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
    }
}

private data class GalleryMonthSection(
    val yearMonth: YearMonth,
    val title: String,
    val progress: String,
    val photos: List<DailyPhoto>,
)

private fun List<DailyPhoto>.toMonthSections(): List<GalleryMonthSection> {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    return groupBy { photo ->
        runCatching {
            YearMonth.from(LocalDate.parse(photo.dateKey))
        }.getOrElse {
            YearMonth.from(photo.capturedAt.atZone(ZoneId.systemDefault()))
        }
    }.map { (yearMonth, monthPhotos) ->
        GalleryMonthSection(
            yearMonth = yearMonth,
            title = monthFormatter.format(yearMonth.atDay(1)),
            progress = "(${monthPhotos.size}/${yearMonth.lengthOfMonth()})",
            photos = monthPhotos,
        )
    }
}
