// /app/src/main/java/com/example/echospace/ui/editor/EditorScreen.kt

package com.example.echospace.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.echospace.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch


@Composable
fun EditorScreen(
    file: DocumentFile,
    viewModel: EditorViewModel,
    themeViewModel: ThemeViewModel,
    onNavigateUp: () -> Unit
) {
    val content by viewModel.content.collectAsState()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var textFieldValue by remember(content) { mutableStateOf(TextFieldValue(content)) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(file) {
        viewModel.loadContent(file)
    }

    // Handle system back button when in edit mode
    BackHandler(enabled = isEditMode) {
        // Save content on back press if in edit mode
        viewModel.saveContent(file, textFieldValue.text) {
            isEditMode = false
            scope.launch {
                snackbarHostState.showSnackbar("File saved")
            }
        }
    }

    Scaffold(
        snackbarHost = {
            // Custom snackbar host with centered, custom width
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .width(maxWidth * 0.7f) // 70% of screen width
                        .padding(bottom = 16.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isEditMode) {
                        viewModel.saveContent(file, textFieldValue.text) {
                            isEditMode = false
                            scope.launch {
                                snackbarHostState.showSnackbar("File saved")
                            }
                        }
                    } else {
                        isEditMode = true
                    }
                }
            ) {
                Icon(
                    if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                    contentDescription = if (isEditMode) "Save" else "Edit"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
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
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
