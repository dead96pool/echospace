// ui/components/CreateEntryDialog.kt
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


@Composable
fun CreateEntryDialog(
    onDismiss: () -> Unit,
    onFileNameConfirmed: (String) -> CreateFileResult,
    errorMessage: String = ""
) {
    var fileName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(errorMessage.isNotEmpty()) }
    val focusRequester = remember { FocusRequester() }

    // Update error state when errorMessage changes
    LaunchedEffect(errorMessage) {
        showError = errorMessage.isNotEmpty()
    }

    // Auto-focus text field when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New File",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        showError = false // Clear error when user types
                    },
                    label = { Text("Enter file name") },
                    placeholder = { Text(
                                            text = "Leave blank for Auto-Name",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        ) },
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
                    val result = onFileNameConfirmed(fileName)
                    if (result == CreateFileResult.SUCCESS) {
                        onDismiss()
                    }
                    // If result is FILENAME_EXISTS, dialog stays open
                }
            ) {
                Text("Create")
            }
        }
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("Cancel")
//            }
//        }
    )
}
