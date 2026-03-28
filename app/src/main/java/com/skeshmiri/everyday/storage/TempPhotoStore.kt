package com.skeshmiri.everyday.storage

import android.content.Context
import java.io.File
import java.time.Clock
import java.time.Duration
import java.util.UUID

class TempPhotoStore(
    context: Context,
    private val clock: Clock,
) {
    private val tempDirectory = File(context.cacheDir, "review-captures").apply {
        mkdirs()
    }

    fun createTempFile(): File = File(
        tempDirectory,
        "capture-${clock.instant().toEpochMilli()}-${UUID.randomUUID()}.jpg",
    )

    fun delete(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }

    fun cleanupStaleFiles(maxAge: Duration = Duration.ofDays(1)) {
        val cutoff = clock.instant().minus(maxAge).toEpochMilli()
        tempDirectory.listFiles().orEmpty()
            .filter { it.isFile && it.lastModified() < cutoff }
            .forEach(File::delete)
    }
}

