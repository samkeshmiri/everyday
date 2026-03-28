package com.skeshmiri.everyday.model

import android.net.Uri
import java.time.Instant

data class DailyPhoto(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateKey: String,
    val capturedAt: Instant,
    val width: Int,
    val height: Int,
)

