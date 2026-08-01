package com.skeshmiri.aphotoaday.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.skeshmiri.aphotoaday.R
import com.skeshmiri.aphotoaday.export.ExportedGalleryVideo

class GalleryVideoExportNotifier(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.export_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = appContext.getString(R.string.export_notification_channel_description)
        }

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun buildForegroundInfo(
        completedFrames: Int,
        totalFrames: Int,
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_photo_of_the_day)
            .setContentTitle(appContext.getString(R.string.export_notification_in_progress_title))
            .setContentText(buildProgressText(completedFrames, totalFrames))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(DailyReminderScheduler.contentPendingIntent(appContext))
            .setProgress(totalFrames.coerceAtLeast(1), completedFrames.coerceAtLeast(0), totalFrames <= 0)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(ONGOING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    fun showCompleted(exportedVideo: ExportedGalleryVideo) {
        if (!canPostStandardNotifications()) return

        NotificationManagerCompat.from(appContext).notify(
            COMPLETED_NOTIFICATION_ID,
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_photo_of_the_day)
                .setContentTitle(appContext.getString(R.string.export_notification_completed_title))
                .setContentText(
                    appContext.getString(
                        R.string.export_notification_completed_body,
                        exportedVideo.frameCount,
                    ),
                )
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(DailyReminderScheduler.contentPendingIntent(appContext))
                .build(),
        )
    }

    fun showFailed(message: String) {
        if (!canPostStandardNotifications()) return

        NotificationManagerCompat.from(appContext).notify(
            COMPLETED_NOTIFICATION_ID,
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_photo_of_the_day)
                .setContentTitle(appContext.getString(R.string.export_notification_failed_title))
                .setContentText(message)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setContentIntent(DailyReminderScheduler.contentPendingIntent(appContext))
                .build(),
        )
    }

    private fun buildProgressText(
        completedFrames: Int,
        totalFrames: Int,
    ): String = if (totalFrames <= 0) {
        appContext.getString(R.string.export_notification_starting_body)
    } else {
        appContext.getString(
            R.string.export_notification_progress_body,
            completedFrames.coerceIn(0, totalFrames),
            totalFrames,
        )
    }

    private fun canPostStandardNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "gallery_video_export"
        const val ONGOING_NOTIFICATION_ID = 2001
        const val COMPLETED_NOTIFICATION_ID = 2002
    }
}
