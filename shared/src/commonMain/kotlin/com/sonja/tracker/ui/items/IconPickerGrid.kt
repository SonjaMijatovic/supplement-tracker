package com.sonja.tracker.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerGrid(
    selectedIconId: String?,
    onIconSelected: (String?) -> Unit,  // null = Skip (clears selection)
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(12.dp)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Choose an icon",
                style = MaterialTheme.typography.labelLarge
            )
            TextButton(onClick = { onIconSelected(null) }) {
                Text("Skip")
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            ItemIcons.allIconIds.forEach { iconId ->
                val isSelected = iconId == selectedIconId
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(tileShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .then(
                            if (isSelected)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, tileShape)
                            else
                                Modifier
                        )
                        .clickable { onIconSelected(iconId) }
                        .semantics {
                            role = Role.Button
                            contentDescription = "$iconId icon${if (isSelected) ", selected" else ""}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ItemIconContent(iconId = iconId, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
