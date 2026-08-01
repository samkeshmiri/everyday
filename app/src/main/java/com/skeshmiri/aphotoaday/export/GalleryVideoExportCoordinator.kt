package com.skeshmiri.aphotoaday.export

import kotlinx.coroutines.flow.Flow

sealed interface GalleryVideoExportState {
    data object Idle : GalleryVideoExportState

    data class Running(
        val workId: String,
        val startedAtEpochMillis: Long,
        val progress: GalleryVideoExportProgress?,
    ) : GalleryVideoExportState

    data class Succeeded(
        val workId: String,
        val exportedVideo: ExportedGalleryVideo,
    ) : GalleryVideoExportState

    data class Failed(
        val workId: String,
        val message: String,
    ) : GalleryVideoExportState
}

interface GalleryVideoExportCoordinator {
    val state: Flow<GalleryVideoExportState>

    fun startExport(fps: Int)
}
