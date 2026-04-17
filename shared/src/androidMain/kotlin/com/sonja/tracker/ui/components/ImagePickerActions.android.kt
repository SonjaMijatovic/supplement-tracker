package com.sonja.tracker.ui.components

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.sonja.tracker.data.db.AppImageStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberImagePickerActions(
    appImageStorage: AppImageStorage,
    onResult: (imagePath: String?) -> Unit
): ImagePickerActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Actual permission state — used to decide whether to launch directly or request first
    var cameraHasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var galleryHasPermission by remember {
        mutableStateOf(hasGalleryPermission(context))
    }
    // Buttons are hidden only on permanent denial (AC4/AC5); unasked or soft-denied still shows
    var cameraPermanentlyDenied by remember { mutableStateOf(false) }
    var galleryPermanentlyDenied by remember { mutableStateOf(false) }

    var cameraImagePath by remember { mutableStateOf<String?>(null) }
    // Guard against rapid double-tap launching two concurrent camera sessions
    var cameraLaunching by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        cameraLaunching = false
        if (success) onResult(cameraImagePath) else onResult(null)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val destPath = appImageStorage.generateImagePath()
            // Copy on IO thread — large photos would ANR on the main thread
            scope.launch(Dispatchers.IO) {
                val success = copyUriToFile(context, uri, destPath)
                withContext(Dispatchers.Main) {
                    if (success) onResult(destPath) else onResult(null)
                }
            }
        } else {
            onResult(null)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraHasPermission = granted
        if (granted) {
            // Auto-launch camera immediately so the user doesn't have to tap again (AC2)
            if (!cameraLaunching) {
                cameraLaunching = true
                val path = appImageStorage.generateImagePath()
                val file = File(path)
                file.parentFile?.mkdirs()
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                cameraImagePath = path
                cameraLauncher.launch(uri)
            }
        } else {
            // We just asked, so rationale=false now means "don't ask again" (permanent denial)
            val activity = context as? android.app.Activity
            if (activity != null &&
                !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            ) {
                cameraPermanentlyDenied = true
            }
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        galleryHasPermission = granted
        if (granted) {
            // Auto-launch gallery immediately so the user doesn't have to tap again (AC3)
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            val activity = context as? android.app.Activity
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE
            if (activity != null && !activity.shouldShowRequestPermissionRationale(perm)) {
                galleryPermanentlyDenied = true
            }
        }
    }

    return ImagePickerActions(
        // Show button unless permanently denied — unasked or soft-denied still get a button (AC4/AC5)
        cameraGranted = !cameraPermanentlyDenied,
        galleryGranted = !galleryPermanentlyDenied,
        launchCamera = {
            if (cameraHasPermission) {
                if (!cameraLaunching) {
                    cameraLaunching = true
                    val path = appImageStorage.generateImagePath()
                    val file = File(path)
                    file.parentFile?.mkdirs()
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    cameraImagePath = path
                    cameraLauncher.launch(uri)
                }
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        launchGallery = {
            if (galleryHasPermission) {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.READ_MEDIA_IMAGES
                else
                    Manifest.permission.READ_EXTERNAL_STORAGE
                galleryPermissionLauncher.launch(perm)
            }
        }
    )
}

private fun hasGalleryPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Copies [uri] content to [destPath]. Returns true on success.
 * On failure, deletes any partial file and returns false.
 */
private fun copyUriToFile(context: Context, uri: Uri, destPath: String): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destPath).use { output ->
                input.copyTo(output)
            }
            true
        } ?: false
    } catch (e: IOException) {
        File(destPath).delete()
        false
    }
}
