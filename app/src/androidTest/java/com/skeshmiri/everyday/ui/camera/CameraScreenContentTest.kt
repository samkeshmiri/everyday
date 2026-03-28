package com.skeshmiri.everyday.ui.camera

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.skeshmiri.everyday.model.DailyPhoto
import com.skeshmiri.everyday.ui.theme.EverydayTheme
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CameraScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsPermissionRequestWhenCameraPermissionIsMissing() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = false,
                        isLoading = false,
                    ),
                    onRequestPermission = {},
                    onOpenGallery = {},
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
        composeRule.onNodeWithText("Open gallery").assertIsDisplayed()
    }

    @Test
    fun showsLockedStateWhenTodaysPhotoExists() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                        todayPhoto = DailyPhoto(
                            id = 1L,
                            uri = Uri.parse("content://everyday/photo/1"),
                            displayName = "2026-03-27_084500.jpg",
                            dateKey = "2026-03-27",
                            capturedAt = Instant.parse("2026-03-27T08:45:00Z"),
                            width = 1200,
                            height = 1600,
                        ),
                    ),
                    onRequestPermission = {},
                    onOpenGallery = {},
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithText("Today's photo is already saved.").assertIsDisplayed()
        composeRule.onNodeWithText("Come back tomorrow for the next one.").assertIsDisplayed()
    }
}
