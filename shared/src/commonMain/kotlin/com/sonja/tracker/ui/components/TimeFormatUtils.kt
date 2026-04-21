package com.sonja.tracker.ui.components

/** Formats "HH:mm" as "8:00 AM" / "9:00 PM" for display. */
fun formatTimeSlot(timeSlot: String): String {
    val parts = timeSlot.split(":")
    if (parts.size != 2) return timeSlot
    val hour = parts[0].trimStart('0').ifEmpty { "0" }.toIntOrNull() ?: return timeSlot
    val minute = parts[1]
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:$minute $amPm"
}
