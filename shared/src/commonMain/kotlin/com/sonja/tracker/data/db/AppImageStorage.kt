package com.sonja.tracker.data.db

expect class AppImageStorage(context: Any? = null) {
    /** Returns the directory path where item images are stored. Creates it if absent. */
    fun getImagesDir(): String

    /** Generates a unique file path for a new image (does not create the file). */
    fun generateImagePath(): String
}
