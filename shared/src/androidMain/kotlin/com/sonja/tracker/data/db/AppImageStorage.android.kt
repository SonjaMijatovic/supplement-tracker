package com.sonja.tracker.data.db

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.UUID

actual class AppImageStorage actual constructor(private val context: Any?) {
    private val ctx get() = requireNotNull(context as? Context) {
        "AppImageStorage requires a non-null Context on Android"
    }

    actual fun getImagesDir(): String {
        val dir = File(ctx.filesDir, "images")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Failed to create images directory: ${dir.absolutePath}")
        }
        return dir.absolutePath
    }

    actual fun generateImagePath(): String =
        File(getImagesDir(), "${UUID.randomUUID()}.jpg").absolutePath
}
