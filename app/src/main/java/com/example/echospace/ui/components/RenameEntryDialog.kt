package com.example.echospace.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun RenameEntryDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    errorMessage: String? = null,
    onErrorClear: () -> Unit = {}
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = currentName,
                selection = TextRange(currentName.length) // Place cursor at the end
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val isError = errorMessage != null

    // Request focus when the dialog appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename File") },
        text = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    // Clear error when user starts typing a different name
                    if (isError && newValue.text != currentName) {
                        onErrorClear()
                    }
                },
                label = { Text("New File Name") },
                modifier = Modifier.focusRequester(focusRequester),
                singleLine = true,
                isError = isError, // Highlight field in red when error exists
                supportingText = {
                    // Display error message below the field
                    if (isError) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textFieldValue.text.isNotBlank()) {
                        onConfirm(textFieldValue.text.trim())
                    }
                },
                enabled = textFieldValue.text.isNotBlank()
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
