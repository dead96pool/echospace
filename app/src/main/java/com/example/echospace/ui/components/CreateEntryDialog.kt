// /app/src/main/java/com/example/echospace/ui/components/CreateEntryDialog.kt

package com.example.echospace.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreateEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New File") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("File Name") },
                modifier = Modifier.focusRequester(focusRequester),
                singleLine = true,
                placeholder = { Text("Leave empty for Auto Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
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
