package com.skeshmiri.everyday.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skeshmiri.everyday.data.DailyPhotoRepository
import com.skeshmiri.everyday.model.DailyPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = true,
    val photos: List<DailyPhoto> = emptyList(),
    val errorMessage: String? = null,
)

class GalleryViewModel(
    private val repository: DailyPhotoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

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
                    )
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
}

