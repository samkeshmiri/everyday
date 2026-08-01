package com.skeshmiri.aphotoaday.export

import android.content.Context
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class WorkManagerGalleryVideoExportCoordinator(
    context: Context,
) : GalleryVideoExportCoordinator {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    override val state: Flow<GalleryVideoExportState> =
        workManager.getWorkInfosForUniqueWorkFlow(GalleryVideoExportWorker.UNIQUE_WORK_NAME)
            .map(::toExportState)
            .distinctUntilChanged()

    override fun startExport(fps: Int) {
        val request = OneTimeWorkRequestBuilder<GalleryVideoExportWorker>()
            .setInputData(
                workDataOf(
                    GalleryVideoExportWorker.KEY_FPS to fps,
                    GalleryVideoExportWorker.KEY_REQUESTED_AT_EPOCH_MILLIS to System.currentTimeMillis(),
                ),
            )
            .build()

        workManager.enqueueUniqueWork(
            GalleryVideoExportWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun toExportState(workInfos: List<WorkInfo>): GalleryVideoExportState {
        val latestWork = workInfos
            .firstOrNull { !it.state.isFinished }
            ?: workInfos.maxByOrNull { workInfo ->
                maxOf(
                    workInfo.progress.getLong(GalleryVideoExportWorker.KEY_STARTED_AT_EPOCH_MILLIS, 0L),
                    workInfo.outputData.getLong(GalleryVideoExportWorker.KEY_REQUESTED_AT_EPOCH_MILLIS, 0L),
                )
            }
            ?: return GalleryVideoExportState.Idle

        val workId = latestWork.id.toString()
        val startedAtEpochMillis = latestWork.progress.getLong(
            GalleryVideoExportWorker.KEY_STARTED_AT_EPOCH_MILLIS,
            latestWork.outputData.getLong(GalleryVideoExportWorker.KEY_REQUESTED_AT_EPOCH_MILLIS, 0L),
        )

        return when (latestWork.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED,
            -> GalleryVideoExportState.Running(
                workId = workId,
                startedAtEpochMillis = startedAtEpochMillis,
                progress = latestWork.progress.toProgress(),
            )

            WorkInfo.State.SUCCEEDED -> {
                val uriString = latestWork.outputData.getString(GalleryVideoExportWorker.KEY_OUTPUT_URI)
                if (uriString.isNullOrBlank()) {
                    GalleryVideoExportState.Failed(
                        workId = workId,
                        message = "Video saved, but the app could not load its details.",
                    )
                } else {
                    GalleryVideoExportState.Succeeded(
                        workId = workId,
                        exportedVideo = ExportedGalleryVideo(
                            uri = Uri.parse(uriString),
                            displayName = latestWork.outputData
                                .getString(GalleryVideoExportWorker.KEY_OUTPUT_DISPLAY_NAME)
                                .orEmpty(),
                            relativePath = latestWork.outputData
                                .getString(GalleryVideoExportWorker.KEY_OUTPUT_RELATIVE_PATH)
                                .orEmpty(),
                            fps = latestWork.outputData.getInt(GalleryVideoExportWorker.KEY_OUTPUT_FPS, 0),
                            frameCount = latestWork.outputData
                                .getInt(GalleryVideoExportWorker.KEY_OUTPUT_FRAME_COUNT, 0),
                            durationSeconds = latestWork.outputData
                                .getDouble(GalleryVideoExportWorker.KEY_OUTPUT_DURATION_SECONDS, 0.0),
                        ),
                    )
                }
            }

            WorkInfo.State.FAILED -> GalleryVideoExportState.Failed(
                workId = workId,
                message = latestWork.outputData
                    .getString(GalleryVideoExportWorker.KEY_ERROR_MESSAGE)
                    ?: "Failed to export the video.",
            )

            WorkInfo.State.CANCELLED -> GalleryVideoExportState.Idle
        }
    }
}

private fun androidx.work.Data.toProgress(): GalleryVideoExportProgress? {
    val totalFrames = getInt(GalleryVideoExportWorker.KEY_TOTAL_FRAMES, 0)
    if (totalFrames <= 0) {
        return null
    }

    return GalleryVideoExportProgress(
        completedFrames = getInt(GalleryVideoExportWorker.KEY_COMPLETED_FRAMES, 0),
        totalFrames = totalFrames,
    )
}
