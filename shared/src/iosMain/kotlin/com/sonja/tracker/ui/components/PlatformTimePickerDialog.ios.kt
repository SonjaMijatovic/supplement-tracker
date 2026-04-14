package com.sonja.tracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
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
                hour = initialHour.toLong()
                minute = initialMinute.toLong()
            }
            date = calendar.dateFromComponents(components) ?: NSDate()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        val h = components.hour.toInt().toString().padStart(2, '0')
                        val m = components.minute.toInt().toString().padStart(2, '0')
                        onTimeSelected("$h:$m")
                    }) { Text("OK") }
                }
            }
        }
    }
}
