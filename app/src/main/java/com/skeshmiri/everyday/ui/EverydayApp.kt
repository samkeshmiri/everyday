package com.skeshmiri.everyday.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skeshmiri.everyday.camera.CameraController
import com.skeshmiri.everyday.di.AppContainer
import com.skeshmiri.everyday.ui.camera.CameraScreen
import com.skeshmiri.everyday.ui.camera.CameraViewModel
import com.skeshmiri.everyday.ui.common.SimpleViewModelFactory
import com.skeshmiri.everyday.ui.gallery.GalleryScreen
import com.skeshmiri.everyday.ui.gallery.GalleryViewModel
import com.skeshmiri.everyday.ui.review.ReviewScreen
import com.skeshmiri.everyday.ui.review.ReviewViewModel
import com.skeshmiri.everyday.ui.viewer.PhotoViewerScreen

@Composable
fun EverydayApp(
    container: AppContainer,
    cameraController: CameraController,
) {
    val navController = rememberNavController()
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
            GalleryViewModel(repository = container.dailyPhotoRepository)
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
            )
        }

        composable(Destinations.Gallery.route) {
            val viewModel: GalleryViewModel = viewModel(factory = galleryFactory)
            GalleryScreen(
                viewModel = viewModel,
                onOpenPhoto = { photo ->
                    navController.navigate(
                        Destinations.Viewer.route(
                            uri = photo.uri.toString(),
                            displayName = photo.displayName,
                        ),
                    )
                },
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
                navArgument(Destinations.Viewer.uriArg) { type = NavType.StringType },
                navArgument(Destinations.Viewer.displayNameArg) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val uri = Uri.parse(
                Uri.decode(backStackEntry.arguments?.getString(Destinations.Viewer.uriArg).orEmpty()),
            )
            val displayName = Uri.decode(
                backStackEntry.arguments?.getString(Destinations.Viewer.displayNameArg).orEmpty(),
            )
            PhotoViewerScreen(
                uri = uri,
                displayName = displayName,
            )
        }
    }
}

private sealed class Destinations(val route: String) {
    data object Camera : Destinations("camera")
    data object Gallery : Destinations("gallery")

    data object Review : Destinations("review") {
        const val dateKeyArg = "dateKey"
        const val tempPathArg = "tempPath"
        const val pattern = "review/{$dateKeyArg}/{$tempPathArg}"

        fun route(dateKey: String, tempPath: String): String =
            "review/$dateKey/${Uri.encode(tempPath)}"
    }

    data object Viewer : Destinations("viewer") {
        const val uriArg = "uri"
        const val displayNameArg = "displayName"
        const val pattern = "viewer/{$uriArg}/{$displayNameArg}"

        fun route(uri: String, displayName: String): String =
            "viewer/${Uri.encode(uri)}/${Uri.encode(displayName)}"
    }
}
