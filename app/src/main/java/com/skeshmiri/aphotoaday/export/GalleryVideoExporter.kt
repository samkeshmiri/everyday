package com.skeshmiri.aphotoaday.export

import android.net.Uri
import com.skeshmiri.aphotoaday.model.DailyPhoto

data class ExportedGalleryVideo(
    val uri: Uri,
    val displayName: String,
    val relativePath: String,
    val fps: Int,
    val frameCount: Int,
    val durationSeconds: Double,
)

data class GalleryVideoExportProgress(
    val completedFrames: Int,
    val totalFrames: Int,
) {
    val fraction: Float
        get() = if (totalFrames <= 0) 0f else completedFrames.toFloat() / totalFrames.toFloat()
}

interface GalleryVideoExporter {
    suspend fun export(
        photos: List<DailyPhoto>,
        fps: Int,
        onProgress: suspend (GalleryVideoExportProgress) -> Unit = {},
    ): ExportedGalleryVideo
}
