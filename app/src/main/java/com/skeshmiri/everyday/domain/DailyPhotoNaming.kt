package com.skeshmiri.everyday.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DailyPhotoNaming {
    private val timeFormatter = DateTimeFormatter.ofPattern("HHmmss")
    private val dateKeyRegex = Regex("""\d{4}-\d{2}-\d{2}""")

    fun todayDateKey(clock: Clock): String = LocalDate.now(clock).toString()

    fun buildDisplayName(clock: Clock, dateKey: String = todayDateKey(clock)): String {
        val timePart = timeFormatter.format(clock.instant().atZone(clock.zone))
        return "${dateKey}_${timePart}.jpg"
    }

    fun extractDateKey(displayName: String, capturedAt: Instant, zoneId: ZoneId): String {
        val prefix = displayName.substringBefore('_')
        return if (dateKeyRegex.matches(prefix)) prefix else LocalDate.ofInstant(capturedAt, zoneId).toString()
    }
}

