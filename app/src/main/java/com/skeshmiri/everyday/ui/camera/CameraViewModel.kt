package com.skeshmiri.everyday.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeshmiri.everyday.camera.CameraController
import com.skeshmiri.everyday.data.DailyPhotoRepository
import com.skeshmiri.everyday.domain.DailyCapturePolicy
import com.skeshmiri.everyday.domain.DailyPhotoNaming
import com.skeshmiri.everyday.model.DailyPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock

data class CameraUiState(
    val hasCameraPermission: Boolean = false,
    val isLoading: Boolean = true,
    val isCapturing: Boolean = false,
    val todayPhoto: DailyPhoto? = null,
    val errorMessage: String? = null,
)

class CameraViewModel(
    private val dailyPhotoRepository: DailyPhotoRepository,
    private val dailyCapturePolicy: DailyCapturePolicy,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun syncPermission(context: Context) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasCameraPermission = hasPermission) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasCameraPermission = granted,
                errorMessage = if (granted) null else "Camera permission is required to take your daily photo.",
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                dailyPhotoRepository.getToday(DailyPhotoNaming.todayDateKey(clock))
            }.onSuccess { todayPhoto ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        todayPhoto = todayPhoto,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load today's photo.",
                    )
                }
            }
        }
    }

    fun capture(
        controller: CameraController,
        onCaptured: (dateKey: String, tempFile: File) -> Unit,
    ) {
        val currentState = _uiState.value
        if (!currentState.hasCameraPermission || currentState.isCapturing) return
        if (!dailyCapturePolicy.canCapture(currentState.todayPhoto)) return

        val dateKey = DailyPhotoNaming.todayDateKey(clock)
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, errorMessage = null) }
            runCatching {
                controller.captureToTemp()
            }.onSuccess { tempFile ->
                _uiState.update { it.copy(isCapturing = false) }
                onCaptured(dateKey, tempFile)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        errorMessage = error.message ?: "Capture failed.",
                    )
                }
            }
        }
    }
}

