package com.skeshmiri.everyday.data

import com.skeshmiri.everyday.model.DailyPhoto
import java.io.File

interface DailyPhotoRepository {
    suspend fun getToday(dateKey: String): DailyPhoto?
    suspend fun listAll(): List<DailyPhoto>
    suspend fun saveFromTemp(tempFile: File, dateKey: String): DailyPhoto
}

