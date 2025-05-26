package com.example.echospace.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.utils.countWords
import com.example.echospace.utils.getRelativeTimeText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiaryEntryItem(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onRename: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDropdownMenu = true }
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document icon
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Note",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Entry details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${countWords(entry.file)} words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = " | ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = getRelativeTimeText(entry.lastModified),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dropdown menu
        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                },
                onClick = {
                    onRename(entry)
                    showDropdownMenu = false
                }
            )

            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                },
                onClick = {
                    onDelete(entry)
                    showDropdownMenu = false
                }
            )
        }
    }
}
