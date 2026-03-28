package com.skeshmiri.everyday.di

import android.content.Context
import com.skeshmiri.everyday.camera.CameraController
import com.skeshmiri.everyday.camera.CameraXCameraController
import com.skeshmiri.everyday.data.DailyPhotoRepository
import com.skeshmiri.everyday.data.MediaStoreDailyPhotoRepository
import com.skeshmiri.everyday.domain.DailyCapturePolicy
import com.skeshmiri.everyday.domain.DefaultDailyCapturePolicy
import com.skeshmiri.everyday.storage.TempPhotoStore
import java.time.Clock

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()
    val tempPhotoStore = TempPhotoStore(appContext, clock)
    val dailyPhotoRepository: DailyPhotoRepository = MediaStoreDailyPhotoRepository(appContext, clock)
    val dailyCapturePolicy: DailyCapturePolicy = DefaultDailyCapturePolicy()

    fun createCameraController(): CameraController =
        CameraXCameraController(
            context = appContext,
            tempPhotoStore = tempPhotoStore,
        )
}

