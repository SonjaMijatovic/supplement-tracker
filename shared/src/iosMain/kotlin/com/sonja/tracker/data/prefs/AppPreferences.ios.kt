package com.sonja.tracker.data.prefs

import platform.Foundation.NSUserDefaults

private const val KEY_INSTALL_DATE = "install_date"
private const val KEY_LAST_SCHEDULED_DATE = "last_scheduled_date"
private const val KEY_NOTIFICATION_BANNER_DISMISSED = "notification_banner_dismissed"

actual class AppPreferences actual constructor(private val context: Any?) {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getInstallDate(): String? = defaults.stringForKey(KEY_INSTALL_DATE)
    actual fun setInstallDate(date: String) { defaults.setObject(date, KEY_INSTALL_DATE) }
    actual fun getLastScheduledDate(): String? = defaults.stringForKey(KEY_LAST_SCHEDULED_DATE)
    actual fun setLastScheduledDate(date: String) { defaults.setObject(date, KEY_LAST_SCHEDULED_DATE) }
    actual fun isNotificationBannerDismissed(): Boolean = defaults.boolForKey(KEY_NOTIFICATION_BANNER_DISMISSED)
    actual fun setNotificationBannerDismissed() { defaults.setBool(true, KEY_NOTIFICATION_BANNER_DISMISSED) }
    actual fun clearNotificationBannerDismissed() { defaults.removeObjectForKey(KEY_NOTIFICATION_BANNER_DISMISSED) }
}
