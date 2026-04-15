package com.sonja.tracker.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sonja.tracker.ui.components.PlatformTimePickerDialog
import com.sonja.tracker.ui.components.rememberBottomSafeAreaPadding
import com.sonja.tracker.ui.navigation.LocalHideNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditSheet(
    onSave: (name: String, weekdayTime: String, weekendTime: String?) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialWeekdayTime: String = "08:00",
    initialWeekendTime: String? = null,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var weekdayTime by remember { mutableStateOf(initialWeekdayTime) }
    var weekendTime by remember { mutableStateOf(initialWeekendTime) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var weekendToggleExpanded by remember { mutableStateOf(initialWeekendTime != null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showWeekendTimePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bottomSafeArea = rememberBottomSafeAreaPadding()
    val hideNavBar = LocalHideNavBar.current

    val pickerOrExpandedActive = weekendToggleExpanded || showTimePicker || showWeekendTimePicker
    LaunchedEffect(pickerOrExpandedActive) {
        hideNavBar.value = pickerOrExpandedActive
    }

    // Restore nav bar when sheet is dismissed
    LaunchedEffect(Unit) {
        try { kotlinx.coroutines.awaitCancellation() } finally { hideNavBar.value = false }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp + bottomSafeArea),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (onDelete != null) "Edit item" else "Add item",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            // Inline pickers replace ALL list rows so the sheet never overflows on iOS.
            // No Dialog wrapper — avoids iOS UIWindow/ModalBottomSheet gesture conflict.
            when {
                showTimePicker -> PlatformTimePickerDialog(
                    initialTime = weekdayTime,
                    onTimeSelected = { selectedTime ->
                        weekdayTime = selectedTime
                        showTimePicker = false
                    },
                    onDismiss = { showTimePicker = false }
                )
                showWeekendTimePicker -> PlatformTimePickerDialog(
                    initialTime = weekendTime ?: weekdayTime,
                    onTimeSelected = { selectedTime ->
                        weekendTime = selectedTime
                        showWeekendTimePicker = false
                    },
                    onDismiss = { showWeekendTimePicker = false }
                )
                else -> {
                    ListItem(
                        headlineContent = { Text("Weekday reminder") },
                        trailingContent = {
                            Text(
                                text = weekdayTime,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            focusManager.clearFocus()
                            showTimePicker = true
                        }
                    )
                    HorizontalDivider()

                    // Weekend toggle row
                    ListItem(
                        headlineContent = { Text("Different time on weekends?") },
                        trailingContent = {
                            Checkbox(checked = weekendToggleExpanded, onCheckedChange = null)
                        },
                        modifier = Modifier
                            .clickable {
                                weekendToggleExpanded = !weekendToggleExpanded
                                if (weekendToggleExpanded && weekendTime == null) {
                                    weekendTime = weekdayTime
                                }
                            }
                            .semantics { role = Role.Checkbox }
                    )

                    if (weekendToggleExpanded) {
                        ListItem(
                            headlineContent = { Text("Weekend reminder") },
                            trailingContent = {
                                Text(
                                    text = weekendTime ?: weekdayTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                focusManager.clearFocus()
                                showWeekendTimePicker = true
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Button(
                onClick = {
                    onSave(name.trim(), weekdayTime, if (weekendToggleExpanded) weekendTime else null)
                    onDismiss()
                },
                // Disabled while a time picker is open so in-progress selections aren't silently
                // dropped. Alternative: add onTimeChanged to PlatformTimePickerDialog to capture
                // the value continuously without requiring OK (Option A).
                enabled = name.isNotBlank() && !showTimePicker && !showWeekendTimePicker,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            if (onDelete != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete item")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete $initialName?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
