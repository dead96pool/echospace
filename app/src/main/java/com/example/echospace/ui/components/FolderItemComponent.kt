// ui/components/FolderItemComponent.kt
package com.example.echospace.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
//import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.echospace.domain.model.FolderItem
import com.example.echospace.utils.getRelativeTimeText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItemComponent(
    item: FolderItem,
    isDarkTheme: Boolean,
    wordCount: Int = 0,
    onClick: () -> Unit,
    onRename: ((FolderItem) -> Unit)? = null,
    onDelete: ((FolderItem) -> Unit)? = null
) {
    val context = LocalContext.current

    // Updated icon colors - very light grey for dark mode
    val iconTint = if (isDarkTheme) {
        Color(0xFFF0F0F0) // Very light grey (almost white) for dark theme
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
        //Color(0xFF757575) // Medium grey for light theme
    }

    var showDropdownMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!item.isFolder && (onRename != null || onDelete != null)) {
                        showDropdownMenu = true
                    }
                }
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with updated color
        Icon(
            imageVector = if (item.isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = if (item.isFolder) "Folder" else "Note",
            modifier = Modifier.size(32.dp),
            tint = iconTint
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Item details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isFolder) FontWeight.Bold else FontWeight.Medium
            )

            if (!item.isFolder && item.entry != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = " | ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = getRelativeTimeText(item.entry.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Update dropdown menu icons too
        if (!item.isFolder) {
            DropdownMenu(
                expanded = showDropdownMenu,
                onDismissRequest = { showDropdownMenu = false }
            ) {
                onRename?.let {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = iconTint // Same very light grey in dark mode
                            )
                        },
                        onClick = {
                            it(item)
                            showDropdownMenu = false
                        }
                    )
                }

                onDelete?.let {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = iconTint // Same very light grey in dark mode
                            )
                        },
                        onClick = {
                            it(item)
                            showDropdownMenu = false
                        }
                    )
                }
            }
        }
    }
}
