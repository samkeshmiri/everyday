package com.skeshmiri.everyday.data

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.skeshmiri.everyday.domain.DailyPhotoNaming
import com.skeshmiri.everyday.model.DailyPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.Clock

class MediaStoreDailyPhotoRepository(
    private val context: Context,
    private val clock: Clock,
) : DailyPhotoRepository {

    private val contentResolver = context.contentResolver
    private val albumRelativePath = "${Environment.DIRECTORY_PICTURES}/Everyday/"
    private val collectionUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val zoneId = clock.zone

    override suspend fun getToday(dateKey: String): DailyPhoto? = withContext(Dispatchers.IO) {
        queryPhotos(
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND " +
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? ESCAPE '\\'",
            selectionArgs = arrayOf(albumRelativePath, "${dateKey}\\_%"),
            sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media._ID} DESC",
            limit = 1,
        ).firstOrNull()
    }

    override suspend fun listAll(): List<DailyPhoto> = withContext(Dispatchers.IO) {
        queryPhotos(
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ?",
            selectionArgs = arrayOf(albumRelativePath),
            sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media._ID} DESC",
        )
    }

    override suspend fun saveFromTemp(tempFile: File, dateKey: String): DailyPhoto = withContext(Dispatchers.IO) {
        require(tempFile.exists()) { "Temp photo does not exist." }
        check(getToday(dateKey) == null) { "A photo has already been saved for $dateKey." }

        val displayName = DailyPhotoNaming.buildDisplayName(clock, dateKey)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, albumRelativePath)
            put(MediaStore.Images.Media.DATE_TAKEN, clock.instant().toEpochMilli())
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(collectionUri, values)
            ?: throw IOException("Failed to create MediaStore record.")

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw FileNotFoundException("Failed to open output stream for $uri.")

            val finalizeValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            contentResolver.update(uri, finalizeValues, null, null)

            queryById(uri.lastPathSegment?.toLongOrNull())
                ?: throw IOException("Saved photo could not be queried back from MediaStore.")
        } catch (error: Throwable) {
            contentResolver.delete(uri, null, null)
            throw error
        }
    }

    private fun queryById(id: Long?): DailyPhoto? {
        if (id == null) return null
        return queryPhotos(
            selection = "${MediaStore.Images.Media._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
            sortOrder = null,
            limit = 1,
        ).firstOrNull()
    }

    private fun queryPhotos(
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String?,
        limit: Int? = null,
    ): List<DailyPhoto> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )

        val photos = mutableListOf<DailyPhoto>()
        contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val row = MediaStorePhotoRow(
                    id = cursor.getLong(idIndex),
                    displayName = cursor.getString(displayNameIndex),
                    dateTakenMillis = cursor.getLongOrNull(dateTakenIndex),
                    dateAddedSeconds = cursor.getLongOrNull(dateAddedIndex),
                    width = cursor.getIntOrZero(widthIndex),
                    height = cursor.getIntOrZero(heightIndex),
                )
                photos += MediaStorePhotoMapper.toDailyPhoto(
                    row = row,
                    zoneId = zoneId,
                    collectionUri = collectionUri,
                )
                if (limit != null && photos.size >= limit) {
                    break
                }
            }
        }
        return photos
    }

    private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
        if (isNull(index)) null else getLong(index)

    private fun android.database.Cursor.getIntOrZero(index: Int): Int =
        if (isNull(index)) 0 else getInt(index)
}

