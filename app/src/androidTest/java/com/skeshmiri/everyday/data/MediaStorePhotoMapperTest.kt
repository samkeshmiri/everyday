package com.skeshmiri.everyday.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class MediaStorePhotoMapperTest {
    @Test
    fun `maps display name prefix into the daily photo`() {
        val photo = MediaStorePhotoMapper.toDailyPhoto(
            row = MediaStorePhotoRow(
                id = 7L,
                displayName = "2026-03-27_084512.jpg",
                dateTakenMillis = 1_774_599_912_000,
                dateAddedSeconds = null,
                width = 1200,
                height = 1600,
            ),
            zoneId = ZoneId.of("Europe/London"),
            collectionUri = Uri.parse("content://everyday/images"),
        )

        assertEquals("2026-03-27", photo.dateKey)
        assertEquals("content://everyday/images/7", photo.uri.toString())
    }

    @Test
    fun `falls back to the timestamp when the filename has no date key`() {
        val photo = MediaStorePhotoMapper.toDailyPhoto(
            row = MediaStorePhotoRow(
                id = 11L,
                displayName = "photo.jpg",
                dateTakenMillis = 1_774_513_600_000,
                dateAddedSeconds = null,
                width = 1000,
                height = 1000,
            ),
            zoneId = ZoneId.of("Europe/London"),
            collectionUri = Uri.parse("content://everyday/images"),
        )

        assertEquals("2026-03-26", photo.dateKey)
    }
}

