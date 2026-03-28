package com.skeshmiri.everyday.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DailyPhotoNamingTest {
    private val zoneId = ZoneId.of("Europe/London")
    private val clock = Clock.fixed(
        Instant.parse("2026-03-27T08:45:12Z"),
        zoneId,
    )

    @Test
    fun `todayDateKey uses the clock zone`() {
        assertEquals("2026-03-27", DailyPhotoNaming.todayDateKey(clock))
    }

    @Test
    fun `buildDisplayName prefixes the local date key`() {
        assertEquals("2026-03-27_084512.jpg", DailyPhotoNaming.buildDisplayName(clock))
    }

    @Test
    fun `extractDateKey falls back to the captured instant when the name is malformed`() {
        val fallback = DailyPhotoNaming.extractDateKey(
            displayName = "photo.jpg",
            capturedAt = Instant.parse("2026-03-26T23:59:00Z"),
            zoneId = zoneId,
        )

        assertEquals("2026-03-26", fallback)
    }
}

