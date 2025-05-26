// ui/components/FolderItemComponent.kt
package com.example.echospace.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.echospace.domain.model.FolderItem
import com.example.echospace.utils.countWords
import com.example.echospace.utils.getRelativeTimeText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItemComponent(
    item: FolderItem,
    onClick: () -> Unit,
    onRename: ((FolderItem) -> Unit)? = null,
    onDelete: ((FolderItem) -> Unit)? = null
) {
    // decide black or white based on theme
    val iconTint = if (isSystemInDarkTheme()) Color.White else Color.Black
    var showMenu by remember { mutableStateOf(false) }
    //var showDropdownMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { if (!item.isFolder) showMenu = true })
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // folder or file icon
        Icon(
            imageVector = if (item.isFolder) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isFolder) FontWeight.Bold else FontWeight.Medium
            )
            if (!item.isFolder && item.entry != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${countWords(item.entry.file)} words",
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

        if (!item.isFolder) {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                onRename?.let { rename ->
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = iconTint
                            )
                        },
                        text = { Text("Rename") },
                        onClick = {
                            rename(item)
                            showMenu = false
                        }
                    )
                }
                onDelete?.let { delete ->
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = iconTint
                            )
                        },
                        text = { Text("Delete") },
                        onClick = {
                            delete(item)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
