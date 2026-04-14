package com.sonja.tracker.data.prefs

expect class AppPreferences(context: Any? = null) {
    fun getInstallDate(): String?
    fun setInstallDate(date: String)
    fun getLastScheduledDate(): String?
    fun setLastScheduledDate(date: String)
    fun isNotificationBannerDismissed(): Boolean
    fun setNotificationBannerDismissed()
    fun clearNotificationBannerDismissed()
}
