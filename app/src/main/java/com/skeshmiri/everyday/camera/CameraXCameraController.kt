package com.skeshmiri.everyday.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.skeshmiri.everyday.storage.TempPhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import android.view.Surface
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraXCameraController(
    private val context: Context,
    private val tempPhotoStore: TempPhotoStore,
) : CameraController {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    override suspend fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        withContext(Dispatchers.Main.immediate) {
            val provider = ProcessCameraProvider.getInstance(context).await(mainExecutor)
            cameraProvider = provider

            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(95)
                .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageCapture,
            )
        }
    }

    override fun unbind() {
        cameraProvider?.unbindAll()
    }

    override suspend fun captureToTemp(): File {
        val capture = imageCapture ?: throw IllegalStateException("Camera is not ready.")
        val outputFile = tempPhotoStore.createTempFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        return suspendCancellableCoroutine { continuation ->
            capture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(outputFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        tempPhotoStore.delete(outputFile)
                        continuation.resumeWithException(IOException("Photo capture failed.", exception))
                    }
                },
            )
        }
    }

    override fun close() {
        unbind()
        cameraExecutor.shutdown()
    }
}

private suspend fun <T> ListenableFuture<T>.await(
    executor: java.util.concurrent.Executor,
): T = suspendCancellableCoroutine { continuation ->
    addListener(
        {
            runCatching { get() }
                .onSuccess(continuation::resume)
                .onFailure(continuation::resumeWithException)
        },
        executor,
    )
}
