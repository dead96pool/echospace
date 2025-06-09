package com.example.echospace.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.echospace.domain.model.CreateFileResult
import com.example.echospace.domain.model.DiaryEntry

@Composable
fun RenameEntryDialog(
    entry: DiaryEntry,
    onDismiss: () -> Unit,
    onRename: (DiaryEntry, String) -> CreateFileResult
) {
    var newFileName by remember { mutableStateOf(entry.title) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus text field when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename File",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = {
                        newFileName = it
                        showError = false // Clear error when user types
                        errorMessage = ""
                    },
                    label = { Text("File name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    isError = showError
                )

                if (showError && errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = onRename(entry, newFileName.trim())
                    when (result) {
                        CreateFileResult.SUCCESS -> onDismiss()
                        CreateFileResult.FILENAME_EXISTS -> {
                            showError = true
                            errorMessage = "Filename already exists!"
                        }
                        CreateFileResult.OTHER_ERROR -> {
                            showError = true
                            errorMessage = "Failed to rename file"
                        }
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
