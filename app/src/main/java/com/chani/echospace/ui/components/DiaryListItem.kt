package com.chani.echospace.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chani.echospace.domain.model.DiaryEntry

@Composable
fun DiaryListItem(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    // Pre-calculate and cache metadata during composition
    val metadataText = remember(entry.wordCount, entry.lastModifiedDisplay) {
        buildMetadataText(entry)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { modifier ->
                if (onLongClick != null) {
                    modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    modifier.clickable { onClick() }
                }
            }
            .padding(vertical = 8.dp, horizontal = 8.dp), // More vertical padding for a spacious feel
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = "File Icon",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.secondaryContainer // Use the theme's primary color for the icon
        )
        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Handle long file names gracefully
            )

            //val metadataText = buildMetadataText(entry)
            if (metadataText.isNotEmpty()) {
                Text(
                    text = metadataText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp  // Smaller font as requested
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),  // Less prominent
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ensure it's only called when cached
private fun buildMetadataText(entry: DiaryEntry): String {
    val parts = mutableListOf<String>()

    // Add word count if available
    entry.wordCount?.let { wordCount ->
        parts.add("$wordCount words")
    }

    // Add last modified date if available
    entry.lastModifiedDisplay?.let { lastModified ->
        parts.add(lastModified)
    }

    return parts.joinToString("  |  ")
}
