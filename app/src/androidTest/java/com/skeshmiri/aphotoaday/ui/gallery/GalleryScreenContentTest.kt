package com.skeshmiri.aphotoaday.ui.gallery

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.skeshmiri.aphotoaday.export.GalleryVideoExportProgress
import com.skeshmiri.aphotoaday.model.DailyPhoto
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class GalleryScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsExportActionWhenPhotosExist() {
        composeRule.setContent {
            EverydayTheme {
                GalleryScreenContent(
                    uiState = GalleryUiState(
                        isLoading = false,
                        photos = listOf(photo(id = 1L)),
                        estimatedDurationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(1, 5),
                    ),
                    onOpenPhoto = {},
                    onOpenExportDialog = {},
                    onOpenGuideSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Export video").assertIsDisplayed()
    }

    @Test
    fun showsGuideSettingsAction() {
        composeRule.setContent {
            EverydayTheme {
                GalleryScreenContent(
                    uiState = GalleryUiState(
                        isLoading = false,
                        photos = emptyList(),
                    ),
                    onOpenPhoto = {},
                    onOpenExportDialog = {},
                    onOpenGuideSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Guide settings").assertIsDisplayed()
    }

    @Test
    fun showsMonthAndPhotoProgressInGalleryHeader() {
        composeRule.setContent {
            EverydayTheme {
                GalleryScreenContent(
                    uiState = GalleryUiState(
                        isLoading = false,
                        photos = listOf(
                            photo(id = 1L, capturedAt = "2026-03-01T08:45:00Z"),
                            photo(id = 2L, capturedAt = "2026-03-02T08:45:00Z"),
                        ),
                    ),
                    onOpenPhoto = {},
                    onOpenExportDialog = {},
                    onOpenGuideSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("March 2026").assertIsDisplayed()
        composeRule.onNodeWithText("(2/31)").assertIsDisplayed()
    }

    @Test
    fun exportDialogUpdatesEstimatedLengthWhenPresetChanges() {
        composeRule.setContent {
            var selectedFps by mutableStateOf(5)
            val photos = List(36) { index -> photo(id = index.toLong()) }

            EverydayTheme {
                GalleryExportDialog(
                    uiState = GalleryUiState(
                        isLoading = false,
                        photos = photos,
                        selectedFps = selectedFps,
                        estimatedDurationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(
                            photoCount = photos.size,
                            fps = selectedFps,
                        ),
                    ),
                    onDismiss = {},
                    onSelectFps = { selectedFps = it },
                    onExport = {},
                    onOpenExportedVideo = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("Estimated length: 7.2 seconds").assertIsDisplayed()
        composeRule.onNodeWithText("3 fps").performClick()
        composeRule.onNodeWithText("Estimated length: 12.0 seconds").assertIsDisplayed()
    }

    @Test
    fun exportDialogShowsPhotoProgressWhileExportRuns() {
        composeRule.setContent {
            EverydayTheme {
                GalleryExportDialog(
                    uiState = GalleryUiState(
                        isLoading = false,
                        photos = List(12) { index -> photo(id = index.toLong()) },
                        isExporting = true,
                        exportProgress = GalleryVideoExportProgress(
                            completedFrames = 3,
                            totalFrames = 12,
                        ),
                        exportStartedAtEpochMillis = System.currentTimeMillis() - 3_000L,
                    ),
                    onDismiss = {},
                    onSelectFps = {},
                    onExport = {},
                    onOpenExportedVideo = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("Exporting video").assertIsDisplayed()
        composeRule.onNodeWithText("3 of 12 photos exported (25%)").assertIsDisplayed()
    }

    private fun photo(
        id: Long,
        capturedAt: String = "2026-03-27T08:45:00Z",
    ) = DailyPhoto(
        id = id,
        uri = Uri.parse("content://everyday/photo/$id"),
        displayName = "2026-03-27_084500.jpg",
        dateKey = capturedAt.substringBefore('T'),
        capturedAt = Instant.parse(capturedAt),
        width = 1200,
        height = 1600,
    )
}
