package com.skeshmiri.aphotoaday.export

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.skeshmiri.aphotoaday.di.AppContainer
import com.skeshmiri.aphotoaday.notifications.GalleryVideoExportNotifier
import com.skeshmiri.aphotoaday.ui.gallery.GalleryVideoExportDefaults

class GalleryVideoExportWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    private val container = AppContainer(appContext)
    private val repository = container.dailyPhotoRepository
    private val exporter = container.galleryVideoExporter
    private val notifier = GalleryVideoExportNotifier(appContext)

    override suspend fun doWork(): Result {
        val fps = inputData.getInt(KEY_FPS, 0)
        if (fps <= 0) {
            return Result.failure(
                workDataOf(
                    KEY_ERROR_MESSAGE to "Frames per second must be greater than zero.",
                    KEY_REQUESTED_AT_EPOCH_MILLIS to inputData.getLong(
                        KEY_REQUESTED_AT_EPOCH_MILLIS,
                        System.currentTimeMillis(),
                    ),
                ),
            )
        }

        val startedAtEpochMillis = inputData.getLong(KEY_REQUESTED_AT_EPOCH_MILLIS, System.currentTimeMillis())
        notifier.ensureChannel()

        return runCatching {
            val photos = GalleryVideoExportDefaults.sortPhotosForExport(repository.listAll())
            require(photos.isNotEmpty()) { "Add at least one photo to export a video." }

            setForeground(
                notifier.buildForegroundInfo(
                    completedFrames = 0,
                    totalFrames = photos.size,
                ),
            )
            setProgress(buildProgressData(0, photos.size, startedAtEpochMillis))

            exporter.export(
                photos = photos,
                fps = fps,
            ) { progress ->
                setProgress(
                    buildProgressData(
                        completedFrames = progress.completedFrames,
                        totalFrames = progress.totalFrames,
                        startedAtEpochMillis = startedAtEpochMillis,
                    ),
                )
                setForeground(
                    notifier.buildForegroundInfo(
                        completedFrames = progress.completedFrames,
                        totalFrames = progress.totalFrames,
                    ),
                )
            }
        }.fold(
            onSuccess = { exportedVideo ->
                notifier.showCompleted(exportedVideo)
                Result.success(buildOutputData(exportedVideo))
            },
            onFailure = { error ->
                val message = error.message ?: "Failed to export the video."
                notifier.showFailed(message)
                Result.failure(
                    workDataOf(
                        KEY_ERROR_MESSAGE to message,
                        KEY_REQUESTED_AT_EPOCH_MILLIS to startedAtEpochMillis,
                    ),
                )
            },
        )
    }

    private fun buildProgressData(
        completedFrames: Int,
        totalFrames: Int,
        startedAtEpochMillis: Long,
    ): Data = workDataOf(
        KEY_COMPLETED_FRAMES to completedFrames,
        KEY_TOTAL_FRAMES to totalFrames,
        KEY_STARTED_AT_EPOCH_MILLIS to startedAtEpochMillis,
    )

    private fun buildOutputData(exportedVideo: ExportedGalleryVideo): Data = workDataOf(
        KEY_REQUESTED_AT_EPOCH_MILLIS to inputData.getLong(
            KEY_REQUESTED_AT_EPOCH_MILLIS,
            System.currentTimeMillis(),
        ),
        KEY_OUTPUT_URI to exportedVideo.uri.toString(),
        KEY_OUTPUT_DISPLAY_NAME to exportedVideo.displayName,
        KEY_OUTPUT_RELATIVE_PATH to exportedVideo.relativePath,
        KEY_OUTPUT_FPS to exportedVideo.fps,
        KEY_OUTPUT_FRAME_COUNT to exportedVideo.frameCount,
        KEY_OUTPUT_DURATION_SECONDS to exportedVideo.durationSeconds,
    )

    companion object {
        const val UNIQUE_WORK_NAME = "gallery_video_export"
        const val KEY_FPS = "fps"
        const val KEY_REQUESTED_AT_EPOCH_MILLIS = "requested_at_epoch_millis"
        const val KEY_COMPLETED_FRAMES = "completed_frames"
        const val KEY_TOTAL_FRAMES = "total_frames"
        const val KEY_STARTED_AT_EPOCH_MILLIS = "started_at_epoch_millis"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_OUTPUT_DISPLAY_NAME = "output_display_name"
        const val KEY_OUTPUT_RELATIVE_PATH = "output_relative_path"
        const val KEY_OUTPUT_FPS = "output_fps"
        const val KEY_OUTPUT_FRAME_COUNT = "output_frame_count"
        const val KEY_OUTPUT_DURATION_SECONDS = "output_duration_seconds"
        const val KEY_ERROR_MESSAGE = "error_message"
    }
}
