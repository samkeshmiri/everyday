# Everyday

Everyday is an offline Android photo app for taking one front-camera selfie per day and storing it in `Pictures/Everyday/`.

## Stack

- Kotlin
- Jetpack Compose
- CameraX
- MediaStore

## Core product rules

- Front camera only
- One saved photo per local calendar day
- Capture into app cache, then review before saving
- Saved photos go to `Pictures/Everyday/`
- In-app gallery is read-only and only shows photos created by this app instance

## Local setup

1. Open the project in Android Studio.
2. Install the Android 17 preview SDK (`Android SDK Platform Cinnamon Bun`) and the latest Android SDK Build-Tools 37.x if prompted.
3. Sync Gradle and run the `app` configuration on a device or emulator with a front camera.

This repo was scaffolded without a verified local Android SDK in the current environment, so first sync/build should be done from Android Studio on a machine with the Android toolchain installed.
