package com.skeshmiri.everyday.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class MediaStoreDailyPhotoRepositoryTest {
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private val createdPhotoIds = mutableListOf<Long>()
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver
    }

    @After
    fun tearDown() {
        createdPhotoIds.forEach { id ->
            contentResolver.delete(
                android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Images.Media.getContentUri(
                        android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY,
                    ),
                    id,
                ),
                null,
                null,
            )
        }
        tempFiles.forEach(File::delete)
    }

    @Test
    fun saveAndGetTodayRoundTripsThroughMediaStore() = runBlocking {
        val repo = repositoryAt("2026-03-27T08:45:12Z")
        val tempFile = createTempJpeg()

        val saved = repo.saveFromTemp(tempFile, "2026-03-27")
        createdPhotoIds += saved.id

        val today = repo.getToday("2026-03-27")

        assertNotNull(today)
        assertEquals(saved.id, today?.id)
        assertEquals("2026-03-27", today?.dateKey)
        assertTrue(saved.displayName.startsWith("2026-03-27_"))
    }

    @Test
    fun recreatedRepositoryStillFindsTodaysPhotoAndGalleryOrdersNewestFirst() = runBlocking {
        val yesterdayRepo = repositoryAt("2026-03-26T08:00:00Z")
        val todayRepo = repositoryAt("2026-03-27T08:45:12Z")

        val yesterday = yesterdayRepo.saveFromTemp(createTempJpeg(), "2026-03-26")
        val today = todayRepo.saveFromTemp(createTempJpeg(), "2026-03-27")
        createdPhotoIds += yesterday.id
        createdPhotoIds += today.id

        val recreatedRepo = repositoryAt("2026-03-27T10:00:00Z")
        val todayPhoto = recreatedRepo.getToday("2026-03-27")
        val photos = recreatedRepo.listAll()

        assertEquals(today.id, todayPhoto?.id)
        assertTrue(photos.size >= 2)
        assertEquals(today.id, photos.first().id)
        assertTrue(photos.any { it.id == yesterday.id })
    }

    private fun repositoryAt(instant: String): MediaStoreDailyPhotoRepository {
        val clock = Clock.fixed(Instant.parse(instant), ZoneId.of("Europe/London"))
        return MediaStoreDailyPhotoRepository(context, clock)
    }

    private fun createTempJpeg(): File {
        val file = File.createTempFile("everyday-test-", ".jpg", context.cacheDir)
        tempFiles += file

        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.CYAN)
        }

        file.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        bitmap.recycle()
        return file
    }
}

