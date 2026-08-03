package com.skeshmiri.aphotoaday.ui.viewer

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PhotoViewerScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tappingImageCallsOnClose() {
        var closed by mutableStateOf(false)

        composeRule.setContent {
            EverydayTheme {
                PhotoViewerScreen(
                    uri = Uri.parse("content://everyday/photo/1"),
                    title = "2026-05-03",
                    contentDescription = "2026-05-03_084500.jpg",
                    capturedAt = null,
                    onClose = { closed = true },
                    onOpenInGallery = {},
                    onShare = {},
                )
            }
        }

        composeRule.onNodeWithTag(PhotoViewerImageTag).performClick()

        composeRule.runOnIdle {
            assertTrue(closed)
        }
    }

    @Test
    fun tappingShareButtonCallsOnShare() {
        var shared by mutableStateOf(false)

        composeRule.setContent {
            EverydayTheme {
                PhotoViewerScreen(
                    uri = Uri.parse("content://everyday/photo/1"),
                    title = "2026-05-03",
                    contentDescription = "2026-05-03_084500.jpg",
                    capturedAt = null,
                    onClose = {},
                    onOpenInGallery = {},
                    onShare = { shared = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Share photo").performClick()

        composeRule.runOnIdle {
            assertTrue(shared)
        }
    }

    @Test
    fun tappingOpenInGalleryButtonCallsOnOpenInGallery() {
        var opened by mutableStateOf(false)

        composeRule.setContent {
            EverydayTheme {
                PhotoViewerScreen(
                    uri = Uri.parse("content://everyday/photo/1"),
                    title = "2026-05-03",
                    contentDescription = "2026-05-03_084500.jpg",
                    capturedAt = null,
                    onClose = {},
                    onOpenInGallery = { opened = true },
                    onShare = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open in gallery").performClick()

        composeRule.runOnIdle {
            assertTrue(opened)
        }
    }

    @Test
    fun swipingLeftMovesToNextPhoto() {
        var sharedPhotoId: Long? = null

        composeRule.setContent {
            EverydayTheme {
                PhotoViewerScreen(
                    photos = listOf(
                        photo(id = 1L),
                        photo(id = 2L),
                        photo(id = 3L),
                    ),
                    initialPhotoId = 1L,
                    onClose = {},
                    onOpenInGallery = {},
                    onShare = { sharedPhotoId = it.id },
                )
            }
        }

        composeRule.onNodeWithTag(PhotoViewerImageTag).performTouchInput {
            swipeLeft()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Share photo").performClick()

        composeRule.runOnIdle {
            assertEquals(2L, sharedPhotoId)
        }
    }

    private fun photo(id: Long) = PhotoViewerItem(
        id = id,
        uri = Uri.parse("content://everyday/photo/$id"),
        title = "2026-05-0$id",
        contentDescription = "2026-05-0${id}_084500.jpg",
        capturedAt = null,
    )
}
