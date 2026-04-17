package com.sonja.tracker.ui.components

import androidx.compose.runtime.Composable
import com.sonja.tracker.data.db.AppImageStorage

data class ImagePickerActions(
    val cameraGranted: Boolean,
    val galleryGranted: Boolean,
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

@Composable
expect fun rememberImagePickerActions(
    appImageStorage: AppImageStorage,
    onResult: (imagePath: String?) -> Unit
): ImagePickerActions
