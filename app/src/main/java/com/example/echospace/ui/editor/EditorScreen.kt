package com.example.echospace.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.echospace.ui.theme.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    file: DocumentFile,
    viewModel: EditorViewModel,
    themeViewModel: ThemeViewModel,
    @Suppress("UNUSED_PARAMETER") onNavigateUp: () -> Unit
) {
    val content by viewModel.content.collectAsState()
    @Suppress("UNUSED_VARIABLE") val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var textFieldValue by remember(content) { mutableStateOf(TextFieldValue(content)) }

    // Modern keyboard and focus management
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Pre-calculate the auto scroll buffer value
    val bufferPx = with(LocalDensity.current) { 80.dp.toPx() }

    // Scroll states for both modes
    val readScrollState = rememberScrollState()
    val editScrollState = rememberScrollState()

    // Snackbar state - LOCAL to EditorScreen
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load content when file changes
    LaunchedEffect(file) {
        viewModel.loadContent(file)
    }

    // Auto-focus and show keyboard when entering edit mode
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            try {
                focusRequester.requestFocus()
                delay(100)
                keyboardController?.show()
            } catch (e: Exception) {
                delay(300)
                keyboardController?.show()
            }
        }
    }

    // Handle system back button when in edit mode
    BackHandler(enabled = isEditMode) {
        viewModel.saveContent(file, textFieldValue.text) {
            isEditMode = false
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "File Saved!",
                    duration = SnackbarDuration.Short // Same duration here
                )
            }
        }
    }

    Scaffold(
        snackbarHost = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { data ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .wrapContentWidth()
                                .widthIn(max = 360.dp)
                                .defaultMinSize(minWidth = 160.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = data.visuals.message,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
                                snackbarHostState.showSnackbar(
                                    message = "File Saved!",
                                    duration = SnackbarDuration.Short // Same duration here
                                )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isEditMode) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                        },
                        onTextLayout = { layoutResult ->
                            if (textFieldValue.selection.start >= 0) {
                                val cursorRect = layoutResult.getCursorRect(textFieldValue.selection.start)

                                val adjustedRect = cursorRect.copy(
                                    bottom = cursorRect.bottom + bufferPx
                                )

                                scope.launch {
                                    bringIntoViewRequester.bringIntoView(adjustedRect)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(editScrollState)
                            .focusRequester(focusRequester)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch {
                                        delay(300)
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(readScrollState)
                    )
                }
            }
        }
    }
}
