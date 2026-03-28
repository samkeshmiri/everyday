package com.skeshmiri.everyday.camera

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.io.Closeable
import java.io.File

interface CameraController : Closeable {
    suspend fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView)
    fun unbind()
    suspend fun captureToTemp(): File
}

