package com.sonja.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDateComponents
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle

@OptIn(ExperimentalForeignApi::class, ExperimentalMaterial3Api::class)
@Composable
actual fun PlatformTimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val datePicker = remember {
        UIDatePicker().apply {
            datePickerMode = UIDatePickerMode.UIDatePickerModeTime
            preferredDatePickerStyle = UIDatePickerStyle.UIDatePickerStyleWheels
            val calendar = NSCalendar.currentCalendar
            val components = NSDateComponents().apply {
                setCalendar(calendar)
                hour = initialHour.toLong()
                minute = initialMinute.toLong()
            }
            date = calendar.dateFromComponents(components)
                ?: run {
                    val fallback = NSDateComponents().apply {
                        setCalendar(calendar)
                        hour = 8L
                        minute = 0L
                    }
                    calendar.dateFromComponents(fallback) ?: calendar.dateFromComponents(NSDateComponents())!!
                }
        }
    }

    // Rendered inline inside the sheet — no Dialog/UIWindow wrapper, which would cause
    // ModalBottomSheet.onDismissRequest to fire on iOS due to UIWindow gesture conflicts.
    Column(modifier = Modifier.fillMaxWidth()) {
        UIKitView(
            factory = { datePicker },
            modifier = Modifier.fillMaxWidth().height(216.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            TextButton(onClick = {
                val calendar = NSCalendar.currentCalendar
                @Suppress("UNCHECKED_CAST")
                val components = calendar.components(
                    NSCalendarUnitHour or NSCalendarUnitMinute,
                    datePicker.date
                )
                // Guard against NSUndefinedDateComponent (~Long.MAX_VALUE) on calendar failure.
                val rawH = components.hour
                val rawM = components.minute
                val hour = if (rawH in 0..23) rawH.toInt() else initialHour
                val minute = if (rawM in 0..59) rawM.toInt() else initialMinute
                onTimeSelected("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
            }) { Text("OK") }
        }
    }
}
