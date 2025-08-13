// /app/src/main/java/com/chani/echospace/ui/components/CreateEntryDialog.kt

package com.chani.echospace.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
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

    // Calendar picker state
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New File") },
        text = {
            Column {
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

                if (showDatePicker) {
                    DatePickerModal(
                        onDateSelected = { dateMillis ->
                            dateMillis?.let {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val selectedDate = dateFormat.format(Date(it))
                                text = selectedDate
                                showDatePicker = false
                            }
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }
            }
        },
        confirmButton = {
            // Row containing calendar icon and create button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calendar icon button
                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Create button
                Button(
                    onClick = {
                        val fileName = if (text.isBlank()) {
                            // Auto-generate with current date
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
            }
        },
        dismissButton = null // Remove cancel button
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = null
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "Select Date for Diary Entry",
                    modifier = Modifier.padding(16.dp)
                )
            },
            showModeToggle = false // Hide the mode toggle for cleaner appearance
        )
    }
}