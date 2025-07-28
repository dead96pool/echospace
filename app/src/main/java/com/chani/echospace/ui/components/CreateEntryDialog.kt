// /app/src/main/java/com/chani/echospace/ui/components/CreateEntryDialog.kt

package com.chani.echospace.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreateEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    errorMessage: String?,
    onErrorClear: () -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val isError = errorMessage != null

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New File") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    // Clear error when user starts typing
                    if (isError && newText.isNotEmpty()) {
                        onErrorClear()
                    }
                },
                label = { Text("File Name") },
                modifier = Modifier.focusRequester(focusRequester),
                singleLine = true,
                placeholder = { Text("Empty for Auto Name") },
                isError = isError, // Highlights the field in red
                trailingIcon = {
                    if (isError) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                supportingText = {
                    // Displays the error message below the field
                    if (isError) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    errorPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val fileName = if (text.isBlank()) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val currentDate = Calendar.getInstance().time
                        dateFormat.format(currentDate)
                    } else {
                        text.trim()
                    }
                    onConfirm(fileName)
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = null // Remove cancel button
    )
}
