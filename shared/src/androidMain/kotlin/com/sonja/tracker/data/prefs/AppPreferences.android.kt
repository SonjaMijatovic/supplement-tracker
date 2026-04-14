package com.sonja.tracker.data.prefs

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "tracker_prefs"
private const val KEY_INSTALL_DATE = "install_date"
private const val KEY_LAST_SCHEDULED_DATE = "last_scheduled_date"
private const val KEY_NOTIFICATION_BANNER_DISMISSED = "notification_banner_dismissed"

actual class AppPreferences actual constructor(context: Any?) {
    private val prefs: SharedPreferences = run {
        val ctx = requireNotNull(context as? Context) {
            "Android AppPreferences requires a non-null Context"
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getInstallDate(): String? = prefs.getString(KEY_INSTALL_DATE, null)
    actual fun setInstallDate(date: String) { prefs.edit().putString(KEY_INSTALL_DATE, date).apply() }
    actual fun getLastScheduledDate(): String? = prefs.getString(KEY_LAST_SCHEDULED_DATE, null)
    actual fun setLastScheduledDate(date: String) { prefs.edit().putString(KEY_LAST_SCHEDULED_DATE, date).apply() }
    actual fun isNotificationBannerDismissed(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_BANNER_DISMISSED, false)
    actual fun setNotificationBannerDismissed() { prefs.edit().putBoolean(KEY_NOTIFICATION_BANNER_DISMISSED, true).apply() }
    actual fun clearNotificationBannerDismissed() { prefs.edit().remove(KEY_NOTIFICATION_BANNER_DISMISSED).apply() }
}
