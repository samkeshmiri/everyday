package com.skeshmiri.aphotoaday.ui.gallery

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasNoClickAction
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

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
                    today = LocalDate.of(2026, 3, 2),
                )
            }
        }

        composeRule.onNodeWithText("March 2026").assertIsDisplayed()
        composeRule.onNodeWithText("(2/31)").assertIsDisplayed()
    }

    @Test
    fun showsNumberedNonClickableSquaresOnlyForMissingDays() {
        composeRule.setContent {
            EverydayTheme {
                GalleryScreenContent(
                    uiState = GalleryUiState(
                        isLoading = false,
                        photos = listOf(
                            photo(id = 1L, capturedAt = "2026-03-01T08:45:00Z"),
                            photo(id = 3L, capturedAt = "2026-03-03T08:45:00Z"),
                        ),
                    ),
                    onOpenPhoto = {},
                    onOpenExportDialog = {},
                    onOpenGuideSettings = {},
                    today = LocalDate.of(2026, 3, 3),
                )
            }
        }

        composeRule.onNodeWithText("2")
            .assertIsDisplayed()
            .assertHasNoClickAction()
        composeRule.onNodeWithText("1").assertDoesNotExist()
        composeRule.onNodeWithText("3").assertDoesNotExist()
    }

    @Test
    fun buildsMissingDaysFromFirstPhotoThroughTodayIncludingEmptyMonths() {
        val sections = listOf(
            photo(id = 1L, capturedAt = "2026-01-30T08:45:00Z"),
            photo(id = 2L, capturedAt = "2026-03-02T08:45:00Z"),
        ).toMonthSections(today = LocalDate.of(2026, 3, 2))

        assertEquals(
            listOf(
                YearMonth.of(2026, 3),
                YearMonth.of(2026, 2),
                YearMonth.of(2026, 1),
            ),
            sections.map { it.yearMonth },
        )

        val februaryItems = sections.single { it.yearMonth == YearMonth.of(2026, 2) }.items
        assertEquals(28, februaryItems.size)
        assertTrue(februaryItems.all { it is GalleryDayItem.Missing })
        assertEquals(LocalDate.of(2026, 2, 28), februaryItems.first().date)
        assertEquals(LocalDate.of(2026, 2, 1), februaryItems.last().date)

        val januaryItems = sections.single { it.yearMonth == YearMonth.of(2026, 1) }.items
        assertEquals(LocalDate.of(2026, 1, 30), januaryItems.last().date)
        assertTrue(januaryItems.none { it.date.isBefore(LocalDate.of(2026, 1, 30)) })

        val marchItems = sections.single { it.yearMonth == YearMonth.of(2026, 3) }.items
        assertEquals(LocalDate.of(2026, 3, 2), marchItems.first().date)
        assertTrue(marchItems.none { it.date.isAfter(LocalDate.of(2026, 3, 2)) })
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
