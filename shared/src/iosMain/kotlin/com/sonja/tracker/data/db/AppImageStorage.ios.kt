package com.sonja.tracker.data.db

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID

actual class AppImageStorage actual constructor(private val context: Any?) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun getImagesDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true
        )
        val supportDir = paths.firstOrNull() as? String
            ?: error("NSApplicationSupportDirectory not found")
        val imagesDir = "$supportDir/images"
        NSFileManager.defaultManager.createDirectoryAtPath(
            imagesDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        // Verify the directory exists after creation (catches silent failures)
        if (!NSFileManager.defaultManager.fileExistsAtPath(imagesDir)) {
            error("Failed to create images directory at $imagesDir")
        }
        return imagesDir
    }

    actual fun generateImagePath(): String =
        "${getImagesDir()}/${NSUUID().UUIDString()}.jpg"
}
