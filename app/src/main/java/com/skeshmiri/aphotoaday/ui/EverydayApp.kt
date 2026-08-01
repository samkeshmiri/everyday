package com.skeshmiri.aphotoaday.ui

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skeshmiri.aphotoaday.camera.CameraController
import com.skeshmiri.aphotoaday.di.AppContainer
import com.skeshmiri.aphotoaday.model.DailyPhoto
import com.skeshmiri.aphotoaday.ui.camera.CameraGuideCalibrationScreen
import com.skeshmiri.aphotoaday.ui.camera.CameraScreen
import com.skeshmiri.aphotoaday.ui.camera.CameraViewModel
import com.skeshmiri.aphotoaday.ui.common.OnResume
import com.skeshmiri.aphotoaday.ui.common.SimpleViewModelFactory
import com.skeshmiri.aphotoaday.ui.gallery.GalleryScreen
import com.skeshmiri.aphotoaday.ui.gallery.GalleryViewModel
import com.skeshmiri.aphotoaday.ui.review.ReviewScreen
import com.skeshmiri.aphotoaday.ui.review.ReviewViewModel
import com.skeshmiri.aphotoaday.ui.viewer.PhotoViewerItem
import com.skeshmiri.aphotoaday.ui.viewer.PhotoViewerScreen
import com.skeshmiri.aphotoaday.ui.viewer.PhotoViewerViewModel

@Composable
fun EverydayApp(
    container: AppContainer,
    cameraController: CameraController,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isCameraOverlayEnabled by container.cameraOverlayPreferences.isOverlayEnabled.collectAsState()
    val cameraGuideSettings by container.cameraOverlayPreferences.guideSettings.collectAsState()
    val hasSeenFirstPhotoInstructions by container.cameraOverlayPreferences.hasSeenFirstPhotoInstructions.collectAsState()
    val cameraFactory = remember(container) {
        SimpleViewModelFactory {
            CameraViewModel(
                dailyPhotoRepository = container.dailyPhotoRepository,
                dailyCapturePolicy = container.dailyCapturePolicy,
                clock = container.clock,
            )
        }
    }
    val galleryFactory = remember(container) {
        SimpleViewModelFactory {
            GalleryViewModel(
                repository = container.dailyPhotoRepository,
                videoExportCoordinator = container.galleryVideoExportCoordinator,
            )
        }
    }
    val viewerFactory = remember(container) {
        SimpleViewModelFactory {
            PhotoViewerViewModel(
                repository = container.dailyPhotoRepository,
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.Camera.route,
    ) {
        composable(Destinations.Camera.route) {
            val viewModel: CameraViewModel = viewModel(factory = cameraFactory)
            CameraScreen(
                viewModel = viewModel,
                cameraController = cameraController,
                onOpenGallery = {
                    navController.navigate(Destinations.Gallery.route) {
                        popUpTo(Destinations.Camera.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onOpenReview = { dateKey, tempPath ->
                    navController.navigate(Destinations.Review.route(dateKey, tempPath))
                },
                showFramingOverlay = isCameraOverlayEnabled,
                guideSettings = cameraGuideSettings,
                onToggleFramingOverlay = container.cameraOverlayPreferences::toggleOverlay,
                hasSeenFirstPhotoInstructions = hasSeenFirstPhotoInstructions,
                onFirstPhotoInstructionsCompleted = container.cameraOverlayPreferences::markFirstPhotoInstructionsSeen,
            )
        }

        composable(Destinations.Gallery.route) {
            val viewModel: GalleryViewModel = viewModel(factory = galleryFactory)
            GalleryScreen(
                viewModel = viewModel,
                onOpenPhoto = { photo ->
                    navController.navigate(Destinations.Viewer.route(photo.id))
                },
                onOpenGuideSettings = {
                    navController.navigate(Destinations.GuideCalibration.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Destinations.GuideCalibration.route) {
            CameraGuideCalibrationScreen(
                cameraController = cameraController,
                dailyPhotoRepository = container.dailyPhotoRepository,
                guideSettings = cameraGuideSettings,
                onSaveGuideSettings = container.cameraOverlayPreferences::setGuideSettings,
                onClose = { navController.popBackStack() },
            )
        }

        composable(
            route = Destinations.Review.pattern,
            arguments = listOf(
                navArgument(Destinations.Review.dateKeyArg) { type = NavType.StringType },
                navArgument(Destinations.Review.tempPathArg) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val dateKey = backStackEntry.arguments?.getString(Destinations.Review.dateKeyArg).orEmpty()
            val tempPath = Uri.decode(
                backStackEntry.arguments?.getString(Destinations.Review.tempPathArg).orEmpty(),
            )
            val reviewFactory = remember(dateKey, tempPath, container) {
                SimpleViewModelFactory {
                    ReviewViewModel(
                        tempPath = tempPath,
                        dateKey = dateKey,
                        repository = container.dailyPhotoRepository,
                        tempPhotoStore = container.tempPhotoStore,
                    )
                }
            }
            val viewModel: ReviewViewModel = viewModel(factory = reviewFactory)
            ReviewScreen(
                viewModel = viewModel,
                onSaved = {
                    navController.navigate(Destinations.Gallery.route) {
                        popUpTo(Destinations.Camera.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onRetake = { navController.popBackStack() },
            )
        }

        composable(
            route = Destinations.Viewer.pattern,
            arguments = listOf(
                navArgument(Destinations.Viewer.idArg) { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong(Destinations.Viewer.idArg) ?: 0L
            val viewModel: PhotoViewerViewModel = viewModel(factory = viewerFactory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val viewerPhotos = remember(uiState.photos, photoId) {
                val loadedPhotos = uiState.photos.map { it.toPhotoViewerItem() }
                loadedPhotos.takeIf { photos -> photos.any { it.id == photoId } }.orEmpty()
            }

            OnResume(viewModel::refresh)
            PhotoViewerScreen(
                photos = viewerPhotos,
                initialPhotoId = photoId,
                onClose = { navController.popBackStack() },
                onShare = { photo -> sharePhoto(context, photo) },
            )
        }
    }
}

private fun DailyPhoto.toPhotoViewerItem(): PhotoViewerItem =
    PhotoViewerItem(
        id = id,
        uri = uri,
        title = dateKey,
        contentDescription = displayName,
        capturedAt = capturedAt,
    )

private fun sharePhoto(context: Context, photo: PhotoViewerItem) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(photo.uri) ?: "image/*"
        putExtra(Intent.EXTRA_STREAM, photo.uri)
        putExtra(Intent.EXTRA_TITLE, photo.contentDescription)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, photo.contentDescription, photo.uri)
    }
    val chooserIntent = Intent.createChooser(shareIntent, null).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(chooserIntent)
}

private sealed class Destinations(val route: String) {
    data object Camera : Destinations("camera")
    data object Gallery : Destinations("gallery")
    data object GuideCalibration : Destinations("guide-calibration")

    data object Review : Destinations("review") {
        const val dateKeyArg = "dateKey"
        const val tempPathArg = "tempPath"
        const val pattern = "review/{$dateKeyArg}/{$tempPathArg}"

        fun route(dateKey: String, tempPath: String): String =
            "review/$dateKey/${Uri.encode(tempPath)}"
    }

    data object Viewer : Destinations("viewer") {
        const val idArg = "id"
        const val pattern = "viewer/{$idArg}"

        fun route(id: Long): String = "viewer/$id"
    }
}
