package com.chani.echospace.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.chani.echospace.ui.components.MarkdownText
import com.chani.echospace.ui.icons.CustomIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    file: DocumentFile,
    viewModel: EditorViewModel = hiltViewModel(),
    startInEditMode: Boolean = false,    // open keyboard with new file
    initialContent: String? = null
) {
    // Collect states from ViewModel instead of using remember
    //val content by viewModel.content.collectAsState()
    val textFieldValue by viewModel.textFieldValue.collectAsState() // NEW: From ViewModel
    val isEditMode by viewModel.isEditMode.collectAsState() // NEW: From ViewModel
    val isLoading by viewModel.isLoading.collectAsState() // NEW: Loading state
    val saveStatus by viewModel.saveStatus.collectAsState() // EXISTING: Save status

    // Modern keyboard and focus management
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Pre-calculate the auto scroll buffer value
    val bufferPx = with(LocalDensity.current) { 80.dp.toPx() }

    // Scroll states for both modes
    val readScrollState = rememberScrollState()
    val editScrollState = rememberScrollState()

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Initialize edit mode only once - UPDATED with guard
    val initialized = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialized.value) {
            viewModel.initializeEditMode(startInEditMode)
            initialized.value = true
        }
    }

    // UPDATED: Load content using ViewModel method instead of direct calls
    LaunchedEffect(initialContent) {
        if (initialContent != null) {
            // This is a new file: set its content and save it immediately
            viewModel.setInitialContentAndSave(file, initialContent)
        } else {
            // This is an existing file: load content from storage
            viewModel.loadContent(file)
        }
    }

    // REMOVED: The old LaunchedEffect(content) synchronization - no longer needed
    // since ViewModel manages textFieldValue directly

    // Load content when file changes (unchanged)
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

    // Handle save status changes for automatic snackbar feedback
    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is SaveStatus.Success -> {
                viewModel.setEditMode(false)
                snackbarHostState.showSnackbar(
                    message = "File Saved!",
                    duration = SnackbarDuration.Short
                )
            }
            is SaveStatus.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Save failed: ${status.message}. Tap save to retry.",
                    duration = SnackbarDuration.Long
                )
            }
            SaveStatus.Saving -> {
                // Stay in edit mode while saving
            }
            SaveStatus.Idle -> {
                // No action needed
            }
        }
    }

    // Handle system back button - now uses ViewModel methods
    BackHandler(enabled = isEditMode) {
        viewModel.saveContent(file, textFieldValue.text)
        //viewModel.setEditMode(false)
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
                    if (isEditMode && saveStatus != SaveStatus.Saving) {
                        viewModel.saveContent(file, textFieldValue.text)
                    } else if (!isEditMode) {
                        viewModel.setEditMode(true)
                    }
                },
                containerColor = when (saveStatus) {
                    is SaveStatus.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
                contentColor = MaterialTheme.colorScheme.background
            ) {
                when {
                    isEditMode && saveStatus == SaveStatus.Saving -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 3.dp
                        )
                    }
                    isEditMode -> {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = when (saveStatus) {
                                is SaveStatus.Error -> "Retry Save"
                                else -> "Save"
                            }
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                }
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
                if (isLoading) {
                    // Show loading indicator while content is being loaded
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    if (isEditMode) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                // UPDATED: Update ViewModel instead of local state
                                viewModel.updateTextContent(newValue)
                            },
                            onTextLayout = { layoutResult ->
                                if (textFieldValue.selection.start >= 0) {
                                    val cursorRect =
                                        layoutResult.getCursorRect(textFieldValue.selection.start)
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
                        // Read-only mode with markdown parsing
                        // UPDATED: Show empty state if no content
                        if (textFieldValue.text.isBlank()) {
                            // ✅ Enhanced empty state with better visual design
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp), // Extra padding for breathing room
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Empty file icon
                                Icon(
                                    imageVector = CustomIcons.UnknownDocument, // Document icon
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .alpha(0.3f), // Reduced alpha for subtle look
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Main empty message
                                Text(
                                    text = "Empty File",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.alpha(0.6f), // Reduced alpha
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Instruction text
                                Text(
                                    text = "Tap the edit button to start writing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.alpha(0.5f), // Even more subtle
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            MarkdownText(
                                text = textFieldValue.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(readScrollState)
                            )
                        }
                    }
                }
            }
        }
    }
}
