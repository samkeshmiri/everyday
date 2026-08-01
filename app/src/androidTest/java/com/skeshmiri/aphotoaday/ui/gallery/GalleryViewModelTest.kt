package com.skeshmiri.aphotoaday.ui.gallery

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skeshmiri.aphotoaday.data.DailyPhotoRepository
import com.skeshmiri.aphotoaday.export.ExportedGalleryVideo
import com.skeshmiri.aphotoaday.export.GalleryVideoExportCoordinator
import com.skeshmiri.aphotoaday.export.GalleryVideoExportState
import com.skeshmiri.aphotoaday.model.DailyPhoto
import com.skeshmiri.aphotoaday.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GalleryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refreshCalculatesTheDefaultEstimatedDuration() = runTest {
        val repository = FakeDailyPhotoRepository(
            photos = List(36) { index ->
                photo(
                    id = index.toLong(),
                    capturedAt = "2026-03-${((index % 28) + 1).toString().padStart(2, '0')}T10:00:00Z",
                )
            },
        )
        val viewModel = GalleryViewModel(
            repository = repository,
            videoExportCoordinator = FakeGalleryVideoExportCoordinator(),
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.selectedFps)
        assertEquals(7.2, viewModel.uiState.value.estimatedDurationSeconds, 0.0)
    }

    @Test
    fun selectFpsUpdatesTheEstimatedDuration() = runTest {
        val viewModel = GalleryViewModel(
            repository = FakeDailyPhotoRepository(
                photos = List(36) { index ->
                    photo(
                        id = index.toLong(),
                        capturedAt = "2026-03-${((index % 28) + 1).toString().padStart(2, '0')}T10:00:00Z",
                    )
                },
            ),
            videoExportCoordinator = FakeGalleryVideoExportCoordinator(),
        )

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.selectFps(3)

        assertEquals(3, viewModel.uiState.value.selectedFps)
        assertEquals(12.0, viewModel.uiState.value.estimatedDurationSeconds, 0.0)
    }

    @Test
    fun exportVideoStartsTheBackgroundExportAndStoresTheExportResult() = runTest {
        val exportCoordinator = FakeGalleryVideoExportCoordinator()
        val viewModel = GalleryViewModel(
            repository = FakeDailyPhotoRepository(
                photos = listOf(
                    photo(id = 3L, capturedAt = "2026-03-29T10:00:00Z"),
                    photo(id = 2L, capturedAt = "2026-03-28T10:00:00Z"),
                    photo(id = 1L, capturedAt = "2026-03-27T10:00:00Z"),
                ),
            ),
            videoExportCoordinator = exportCoordinator,
        )

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.selectFps(8)
        viewModel.exportVideo()
        exportCoordinator.emit(
            GalleryVideoExportState.Succeeded(
                workId = "export-1",
                exportedVideo = exportedVideo(fps = 8, frameCount = 3),
            ),
        )
        advanceUntilIdle()

        assertEquals(8, exportCoordinator.lastStartedFps)
        assertNotNull(viewModel.uiState.value.exportedVideo)
        assertNull(viewModel.uiState.value.exportErrorMessage)
    }

    @Test
    fun exportVideoSurfacesExporterFailures() = runTest {
        val exportCoordinator = FakeGalleryVideoExportCoordinator()
        val viewModel = GalleryViewModel(
            repository = FakeDailyPhotoRepository(
                photos = listOf(photo(id = 1L, capturedAt = "2026-03-27T10:00:00Z")),
            ),
            videoExportCoordinator = exportCoordinator,
        )

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.exportVideo()
        exportCoordinator.emit(
            GalleryVideoExportState.Failed(
                workId = "export-2",
                message = "Encoder failed.",
            ),
        )
        advanceUntilIdle()

        assertEquals("Encoder failed.", viewModel.uiState.value.exportErrorMessage)
        assertNull(viewModel.uiState.value.exportedVideo)
    }

    private class FakeDailyPhotoRepository(
        private val photos: List<DailyPhoto>,
    ) : DailyPhotoRepository {
        override suspend fun getToday(dateKey: String): DailyPhoto? = photos.firstOrNull { it.dateKey == dateKey }

        override suspend fun listAll(): List<DailyPhoto> = photos

        override suspend fun saveFromTemp(tempFile: File, dateKey: String): DailyPhoto {
            throw UnsupportedOperationException("Not needed for GalleryViewModel tests.")
        }
    }

    private class FakeGalleryVideoExportCoordinator : GalleryVideoExportCoordinator {
        private val mutableState = MutableStateFlow<GalleryVideoExportState>(GalleryVideoExportState.Idle)

        override val state = mutableState.asStateFlow()
        var lastStartedFps: Int? = null

        override fun startExport(fps: Int) {
            lastStartedFps = fps
        }

        fun emit(state: GalleryVideoExportState) {
            mutableState.value = state
        }
    }

    private fun photo(
        id: Long,
        capturedAt: String,
    ) = DailyPhoto(
        id = id,
        uri = Uri.parse("content://everyday/photo/$id"),
        displayName = "2026-03-27_084500.jpg",
        dateKey = "2026-03-27",
        capturedAt = Instant.parse(capturedAt),
        width = 1200,
        height = 1600,
    )

    private fun exportedVideo(
        fps: Int,
        frameCount: Int,
    ) = ExportedGalleryVideo(
        uri = Uri.parse("content://everyday/video/exported"),
        displayName = "Everyday_2026-03-27_to_2026-03-29_${fps}fps.mp4",
        relativePath = "Movies/Everyday/",
        fps = fps,
        frameCount = frameCount,
        durationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(frameCount, fps),
    )
}
