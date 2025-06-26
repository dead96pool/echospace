// /app/src/main/java/com/example/echospace/ui/editor/EditorScreen.kt

package com.example.echospace.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

@Composable
fun EditorScreen(
    file: DocumentFile,
    viewModel: EditorViewModel,
    onNavigateUp: () -> Unit
) {
    val content by viewModel.content.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var textFieldValue by remember(content) { mutableStateOf(TextFieldValue(content)) }

    LaunchedEffect(file) {
        viewModel.loadContent(file)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isEditMode) {
                    viewModel.saveContent(file, textFieldValue.text) {
                        isEditMode = false
                        onNavigateUp() // Go back after saving
                    }
                } else {
                    isEditMode = true
                }
            }) {
                Icon(
                    if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                    contentDescription = if (isEditMode) "Save" else "Edit"
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (isEditMode) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            } else {
                Text(text = content)
            }
        }
    }
}
