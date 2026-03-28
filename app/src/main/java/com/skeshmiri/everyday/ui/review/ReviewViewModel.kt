package com.skeshmiri.everyday.ui.review

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeshmiri.everyday.data.DailyPhotoRepository
import com.skeshmiri.everyday.model.DailyPhoto
import com.skeshmiri.everyday.storage.TempPhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ReviewUiState(
    val dateKey: String,
    val tempFile: File?,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class ReviewViewModel(
    tempPath: String,
    private val dateKey: String,
    private val repository: DailyPhotoRepository,
    private val tempPhotoStore: TempPhotoStore,
) : ViewModel() {
    private val initialFile = File(tempPath).takeIf(File::exists)
    private val _uiState = MutableStateFlow(
        ReviewUiState(
            dateKey = dateKey,
            tempFile = initialFile,
            errorMessage = if (initialFile == null) "The pending photo is no longer available." else null,
        ),
    )
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun save(onSaved: (DailyPhoto) -> Unit) {
        val tempFile = _uiState.value.tempFile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                repository.saveFromTemp(tempFile, dateKey)
            }.onSuccess { dailyPhoto ->
                tempPhotoStore.delete(tempFile)
                _uiState.update { it.copy(isSaving = false, tempFile = null) }
                onSaved(dailyPhoto)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Save failed.",
                    )
                }
            }
        }
    }

    fun discard(onDiscarded: () -> Unit) {
        _uiState.value.tempFile?.let(tempPhotoStore::delete)
        _uiState.update { it.copy(tempFile = null) }
        onDiscarded()
    }

    val tempUri
        get() = _uiState.value.tempFile?.toUri()
}
