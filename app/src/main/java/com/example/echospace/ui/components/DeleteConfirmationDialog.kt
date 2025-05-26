package com.example.echospace.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.echospace.domain.model.DiaryEntry

@Composable
fun DeleteConfirmationDialog(
    entry: DiaryEntry,
    onDismiss: () -> Unit,
    onConfirm: (DiaryEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Entry") },
        text = { Text("Are you sure you want to delete '${entry.title}'?") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(entry)
                    onDismiss()
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
