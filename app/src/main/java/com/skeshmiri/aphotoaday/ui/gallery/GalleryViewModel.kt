package com.skeshmiri.aphotoaday.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeshmiri.aphotoaday.data.DailyPhotoRepository
import com.skeshmiri.aphotoaday.export.ExportedGalleryVideo
import com.skeshmiri.aphotoaday.export.GalleryVideoExportCoordinator
import com.skeshmiri.aphotoaday.export.GalleryVideoExportProgress
import com.skeshmiri.aphotoaday.export.GalleryVideoExportState
import com.skeshmiri.aphotoaday.model.DailyPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = true,
    val photos: List<DailyPhoto> = emptyList(),
    val errorMessage: String? = null,
    val selectedFps: Int = GalleryVideoExportDefaults.defaultFps,
    val estimatedDurationSeconds: Double = 0.0,
    val isExporting: Boolean = false,
    val exportProgress: GalleryVideoExportProgress? = null,
    val exportStartedAtEpochMillis: Long? = null,
    val exportErrorMessage: String? = null,
    val exportedVideo: ExportedGalleryVideo? = null,
    val terminalExportWorkId: String? = null,
)

class GalleryViewModel(
    private val repository: DailyPhotoRepository,
    private val videoExportCoordinator: GalleryVideoExportCoordinator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    private var dismissedTerminalWorkId: String? = null

    init {
        viewModelScope.launch {
            videoExportCoordinator.state.collect { exportState ->
                _uiState.update { currentState ->
                    currentState.withExportState(exportState, dismissedTerminalWorkId)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.listAll()
            }.onSuccess { photos ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        photos = photos,
                    ).withEstimatedDuration()
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load photos.",
                    )
                }
            }
        }
    }

    fun selectFps(fps: Int) {
        if (fps !in GalleryVideoExportDefaults.fpsPresets) {
            return
        }

        _uiState.update {
            it.copy(
                selectedFps = fps,
                exportErrorMessage = null,
                exportedVideo = null,
            ).withEstimatedDuration()
        }
    }

    fun clearExportFeedback() {
        dismissedTerminalWorkId = _uiState.value.terminalExportWorkId
        _uiState.update {
            it.copy(
                exportErrorMessage = null,
                exportedVideo = null,
                terminalExportWorkId = null,
            )
        }
    }

    fun exportVideo() {
        val snapshot = _uiState.value
        if (snapshot.isExporting) {
            return
        }

        if (snapshot.photos.isEmpty()) {
            _uiState.update {
                it.copy(exportErrorMessage = "Add at least one photo to export a video.")
            }
            return
        }

        dismissedTerminalWorkId = null
        _uiState.update {
            it.copy(
                isExporting = true,
                exportProgress = null,
                exportStartedAtEpochMillis = System.currentTimeMillis(),
                exportErrorMessage = null,
                exportedVideo = null,
                terminalExportWorkId = null,
            )
        }
        videoExportCoordinator.startExport(snapshot.selectedFps)
    }
}

private fun GalleryUiState.withEstimatedDuration(): GalleryUiState =
    copy(
        estimatedDurationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(
            photoCount = photos.size,
            fps = selectedFps,
        ),
    )

private fun GalleryUiState.withExportState(
    exportState: GalleryVideoExportState,
    dismissedTerminalWorkId: String?,
): GalleryUiState = when (exportState) {
    GalleryVideoExportState.Idle -> copy(
        isExporting = false,
        exportProgress = null,
        exportStartedAtEpochMillis = null,
    )

    is GalleryVideoExportState.Running -> copy(
        isExporting = true,
        exportProgress = exportState.progress,
        exportStartedAtEpochMillis = exportState.startedAtEpochMillis,
        exportErrorMessage = null,
        exportedVideo = null,
        terminalExportWorkId = null,
    )

    is GalleryVideoExportState.Succeeded ->
        if (dismissedTerminalWorkId == exportState.workId) {
            copy(
                isExporting = false,
                exportProgress = null,
                exportStartedAtEpochMillis = null,
            )
        } else {
            copy(
                isExporting = false,
                exportProgress = null,
                exportStartedAtEpochMillis = null,
                exportErrorMessage = null,
                exportedVideo = exportState.exportedVideo,
                terminalExportWorkId = exportState.workId,
            )
        }

    is GalleryVideoExportState.Failed ->
        if (dismissedTerminalWorkId == exportState.workId) {
            copy(
                isExporting = false,
                exportProgress = null,
                exportStartedAtEpochMillis = null,
            )
        } else {
            copy(
                isExporting = false,
                exportProgress = null,
                exportStartedAtEpochMillis = null,
                exportErrorMessage = exportState.message,
                exportedVideo = null,
                terminalExportWorkId = exportState.workId,
            )
        }
}
