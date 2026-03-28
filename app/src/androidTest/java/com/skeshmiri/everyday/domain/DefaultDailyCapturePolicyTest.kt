package com.skeshmiri.everyday.domain

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skeshmiri.everyday.model.DailyPhoto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DefaultDailyCapturePolicyTest {
    private val policy = DefaultDailyCapturePolicy()

    @Test
    fun allowsCaptureWhenThereIsNoPhotoForToday() {
        assertTrue(policy.canCapture(todayPhoto = null))
    }

    @Test
    fun blocksCaptureWhenAPhotoAlreadyExistsForToday() {
        val existingPhoto = DailyPhoto(
            id = 1L,
            uri = Uri.parse("content://everyday/photo/1"),
            displayName = "2026-03-27_080000.jpg",
            dateKey = "2026-03-27",
            capturedAt = Instant.parse("2026-03-27T08:00:00Z"),
            width = 1200,
            height = 1600,
        )

        assertFalse(policy.canCapture(existingPhoto))
    }
}
