package com.sonja.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sonja.tracker.domain.model.Item

/**
 * Renders a single item row.
 *
 * @param isLogRow  When true, the row is used for log-tap interactions (Today screen).
 *                  Applies Role.Checkbox semantics and "[name], pending" / "[name], already logged"
 *                  content descriptions. When false (default), uses Role.Button + "tap to edit"
 *                  semantics for the Items screen.
 * @param isLogged  When true, shows the logged state (tonal background + check icon) and
 *                  disables tap interaction. Only relevant when [isLogRow] = true.
 * @param onClick   Tap handler. Null means non-interactive.
 */
@Composable
fun ItemRow(
    item: Item,
    modifier: Modifier = Modifier,
    isLogRow: Boolean = false,
    isLogged: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val interactionModifier = when {
        isLogRow && isLogged -> {
            // Logged state: non-tappable, accessible as "already logged"
            Modifier.semantics {
                role = Role.Checkbox
                contentDescription = "${item.name}, already logged"
            }
        }
        isLogRow && onClick != null -> {
            // Pending state: tappable, accessible as "pending"
            Modifier
                .clickable { onClick() }
                .semantics {
                    role = Role.Checkbox
                    contentDescription = "${item.name}, pending"
                }
        }
        onClick != null -> {
            // Items screen edit mode (legacy)
            Modifier
                .clickable { onClick() }
                .semantics {
                    role = Role.Button
                    contentDescription = "${item.name}, tap to edit"
                }
        }
        else -> Modifier
    }

    val backgroundModifier = if (isLogRow && isLogged) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(backgroundModifier)
            .padding(vertical = 4.dp)
            .then(interactionModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .semantics {
                    contentDescription = when {
                        item.imagePath != null -> "${item.name} photo"
                        item.iconId != null -> "${item.name} icon"
                        else -> "No icon"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                item.imagePath != null -> {
                    var imageLoadFailed by remember(item.imagePath) { mutableStateOf(false) }
                    if (imageLoadFailed && item.iconId != null) {
                        com.sonja.tracker.ui.items.ItemIconContent(
                            iconId = item.iconId,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        AsyncImage(
                            model = item.imagePath,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            onError = { imageLoadFailed = true }
                        )
                    }
                }
                item.iconId != null -> com.sonja.tracker.ui.items.ItemIconContent(
                    iconId = item.iconId,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        // Check indicator — only shown when logged in Today screen context
        if (isLogRow && isLogged) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
