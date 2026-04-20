package com.sonja.tracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonja.tracker.domain.model.TimeGroup

@Composable
fun TimeGroupSection(
    group: TimeGroup,
    onLogItem: (itemId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Group header: time label + pending badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimeSlot(group.timeSlot),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            val pendingCount = group.items.size - group.loggedItemIds.size
            // Story 3.2: badge reflects actual pending count; overdue/done states added in story 3.3
            Text(
                text = "$pendingCount pending",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        group.items.forEach { item ->
            val logged = item.id in group.loggedItemIds
            ItemRow(
                item = item,
                modifier = Modifier.padding(horizontal = 16.dp),
                isLogRow = true,
                isLogged = logged,
                onClick = if (logged) null else { { onLogItem(item.id) } }
            )
        }
    }
}

/** Formats "HH:mm" as "8:00 AM" / "9:00 PM" for display. */
private fun formatTimeSlot(timeSlot: String): String {
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
