// ui/components/CreateEntryDialog.kt
package com.example.echospace.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateEntryDialog(
    onDismiss: () -> Unit,
    onFileNameConfirmed: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Entry") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Enter file name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                TextButton(
                    onClick = {
                        onFileNameConfirmed("")
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auto Name")
                }
                Button(
                    onClick = {
                        onFileNameConfirmed(fileName)
                        onDismiss()
                    },
                    enabled = fileName.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create")
                }
            }
        }
    )
}
