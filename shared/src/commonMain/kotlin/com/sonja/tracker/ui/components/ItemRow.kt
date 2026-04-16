package com.sonja.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sonja.tracker.domain.model.Item

@Composable
fun ItemRow(
    item: Item,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .clickable { onClick() }
            .semantics {
                role = Role.Button
                contentDescription = "${item.name}, tap to edit"
            }
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp)
            .then(clickModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .semantics {
                    contentDescription = if (item.iconId != null) "${item.name} icon" else "No icon"
                },
            contentAlignment = Alignment.Center
        ) {
            if (item.iconId != null) {
                com.sonja.tracker.ui.items.ItemIconContent(iconId = item.iconId, modifier = Modifier.size(24.dp))
            }
            // item.imagePath rendering added in Story 2.6
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
