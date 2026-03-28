package com.skeshmiri.everyday.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.skeshmiri.everyday.domain.DailyPhotoNaming
import com.skeshmiri.everyday.model.DailyPhoto
import java.time.Instant
import java.time.ZoneId

object MediaStorePhotoMapper {
    fun toDailyPhoto(
        row: MediaStorePhotoRow,
        zoneId: ZoneId,
        collectionUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    ): DailyPhoto {
        val capturedAt = row.dateTakenMillis?.let(Instant::ofEpochMilli)
            ?: Instant.ofEpochSecond(row.dateAddedSeconds ?: 0L)
        return DailyPhoto(
            id = row.id,
            uri = ContentUris.withAppendedId(collectionUri, row.id),
            displayName = row.displayName,
            dateKey = DailyPhotoNaming.extractDateKey(row.displayName, capturedAt, zoneId),
            capturedAt = capturedAt,
            width = row.width,
            height = row.height,
        )
    }
}

