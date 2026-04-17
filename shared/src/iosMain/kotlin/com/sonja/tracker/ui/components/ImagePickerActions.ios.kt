package com.sonja.tracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sonja.tracker.data.db.AppImageStorage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSFileManager
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// Returns true when permission is not permanently denied (NotDetermined and Authorized both show the button)
private fun cameraPermissionAllowed(): Boolean {
    val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
    return status != AVAuthorizationStatusDenied && status != AVAuthorizationStatusRestricted
}

// Returns true when permission is not permanently denied
private fun galleryPermissionAllowed(): Boolean {
    val status = PHPhotoLibrary.authorizationStatus()
    return status != PHAuthorizationStatusDenied && status != PHAuthorizationStatusRestricted
}

private fun cameraPermissionGranted(): Boolean =
    AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized

private fun galleryPermissionGranted(): Boolean =
    PHPhotoLibrary.authorizationStatus() == PHAuthorizationStatusAuthorized

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerActions(
    appImageStorage: AppImageStorage,
    onResult: (imagePath: String?) -> Unit
): ImagePickerActions {
    var cameraGranted by remember { mutableStateOf(cameraPermissionAllowed()) }
    var galleryGranted by remember { mutableStateOf(galleryPermissionAllowed()) }

    // Retain delegate to prevent GC while picker is presented
    val retainedDelegate = remember { mutableStateOf<NSObject?>(null) }

    return ImagePickerActions(
        cameraGranted = cameraGranted,
        galleryGranted = galleryGranted,
        launchCamera = {
            // Guard against double-tap while a picker session is already active
            if (retainedDelegate.value != null) return@ImagePickerActions
            if (cameraPermissionGranted()) {
                val delegate = PickerDelegate(appImageStorage, onResult) {
                    retainedDelegate.value = null
                }
                retainedDelegate.value = delegate
                presentImagePicker(
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                    delegate
                )
            } else {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    // Callback is on an arbitrary thread — dispatch to main before touching state or UIKit
                    dispatch_async(dispatch_get_main_queue()) {
                        if (granted) {
                            cameraGranted = true
                            // Auto-launch camera immediately after grant (AC2)
                            if (retainedDelegate.value == null) {
                                val delegate = PickerDelegate(appImageStorage, onResult) {
                                    retainedDelegate.value = null
                                }
                                retainedDelegate.value = delegate
                                presentImagePicker(
                                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                                    delegate
                                )
                            }
                        } else {
                            cameraGranted = cameraPermissionAllowed()
                        }
                    }
                }
            }
        },
        launchGallery = {
            // Guard against double-tap while a picker session is already active
            if (retainedDelegate.value != null) return@ImagePickerActions
            if (galleryPermissionGranted()) {
                val delegate = PickerDelegate(appImageStorage, onResult) {
                    retainedDelegate.value = null
                }
                retainedDelegate.value = delegate
                presentImagePicker(
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                    delegate
                )
            } else {
                PHPhotoLibrary.requestAuthorization { status ->
                    // Callback is on an arbitrary thread — dispatch to main before touching state or UIKit
                    dispatch_async(dispatch_get_main_queue()) {
                        if (status == PHAuthorizationStatusAuthorized) {
                            galleryGranted = true
                            // Auto-launch gallery immediately after grant (AC3)
                            if (retainedDelegate.value == null) {
                                val delegate = PickerDelegate(appImageStorage, onResult) {
                                    retainedDelegate.value = null
                                }
                                retainedDelegate.value = delegate
                                presentImagePicker(
                                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                                    delegate
                                )
                            }
                        } else {
                            galleryGranted = galleryPermissionAllowed()
                        }
                    }
                }
            }
        }
    )
}

private class PickerDelegate(
    private val appImageStorage: AppImageStorage,
    private val onResult: (imagePath: String?) -> Unit,
    private val onDone: () -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]
            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage]) as? UIImage
        val path = image?.let {
            val destPath = appImageStorage.generateImagePath()
            val data = UIImageJPEGRepresentation(it, 0.85)
            val written = data?.let { d ->
                NSFileManager.defaultManager.createFileAtPath(destPath, d, null)
            } == true
            if (written) destPath else null
        }
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(path)
        onDone()
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(null)
        onDone()
    }
}

@Suppress("DEPRECATION")
private fun presentImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    delegate: PickerDelegate
) {
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    // Use the topmost presented controller to avoid "already presenting" crash
    val presenter = rootVC.presentedViewController ?: rootVC
    val picker = UIImagePickerController()
    picker.sourceType = sourceType
    picker.allowsEditing = false
    picker.delegate = delegate
    presenter.presentViewController(picker, animated = true, completion = null)
}
