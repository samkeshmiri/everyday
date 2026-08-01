package com.skeshmiri.aphotoaday.di

import android.content.Context
import com.skeshmiri.aphotoaday.camera.CameraController
import com.skeshmiri.aphotoaday.camera.CameraXCameraController
import com.skeshmiri.aphotoaday.data.DailyPhotoRepository
import com.skeshmiri.aphotoaday.data.MediaStoreDailyPhotoRepository
import com.skeshmiri.aphotoaday.domain.DailyCapturePolicy
import com.skeshmiri.aphotoaday.domain.DefaultDailyCapturePolicy
import com.skeshmiri.aphotoaday.export.MediaStoreGalleryVideoExporter
import com.skeshmiri.aphotoaday.export.WorkManagerGalleryVideoExportCoordinator
import com.skeshmiri.aphotoaday.export.GalleryVideoExporter
import com.skeshmiri.aphotoaday.export.GalleryVideoExportCoordinator
import com.skeshmiri.aphotoaday.notifications.DailyReminderScheduler
import com.skeshmiri.aphotoaday.storage.TempPhotoStore
import com.skeshmiri.aphotoaday.ui.camera.CameraOverlayPreferences
import java.time.Clock

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()
    val tempPhotoStore = TempPhotoStore(appContext, clock)
    val dailyPhotoRepository: DailyPhotoRepository = MediaStoreDailyPhotoRepository(appContext, clock)
    val galleryVideoExporter: GalleryVideoExporter = MediaStoreGalleryVideoExporter(appContext, clock)
    val galleryVideoExportCoordinator: GalleryVideoExportCoordinator =
        WorkManagerGalleryVideoExportCoordinator(appContext)
    val dailyCapturePolicy: DailyCapturePolicy = DefaultDailyCapturePolicy()
    val dailyReminderScheduler = DailyReminderScheduler(appContext, clock)
    val cameraOverlayPreferences = CameraOverlayPreferences(appContext)

    fun createCameraController(): CameraController =
        CameraXCameraController(
            context = appContext,
            tempPhotoStore = tempPhotoStore,
        )
}
