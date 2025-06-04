package com.example.echospace.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.domain.repository.DiaryRepository
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    file: File,
    diaryRepository: DiaryRepository,
    startInEditMode: Boolean,
    isDarkTheme: Boolean,
    @Suppress("UNUSED_PARAMETER") onThemeToggle: () -> Unit,
    onNavigateUp: () -> Unit,
    currentPath: String = "",
    onPathChanged: (String) -> Unit = {}
) {
    var isEditMode by remember { mutableStateOf(startInEditMode) }

    // Use TextFieldValue to track cursor position
    var textFieldValue by remember {
        val content = diaryRepository.readEntryContent(file)
        mutableStateOf(
            if (startInEditMode && content.startsWith("# ")) {
                // Look for the pattern: first line + double newline
                val doubleNewlineIndex = content.indexOf("\n\n")
                if (doubleNewlineIndex != -1) {
                    // Position cursor after the double newline
                    val cursorPosition = doubleNewlineIndex + 2
                    TextFieldValue(content, TextRange(cursorPosition))
                } else {
                    // Fallback: position after first line
                    val firstLineEnd = content.indexOf('\n')
                    val cursorPosition = if (firstLineEnd != -1) firstLineEnd + 1 else content.length
                    TextFieldValue(content, TextRange(cursorPosition))
                }
            } else {
                TextFieldValue(content)
            }
        )
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Scroll state ONLY for view mode
    val scrollState = rememberScrollState()

    // Track keyboard visibility
    val isKeyboardOpen by rememberKeyboardState()

    // Track text field focus and layout
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // BringIntoViewRequester for automatic scrolling
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Request focus only when entering edit mode
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    // Function to bring cursor into view with buffer lines
    fun bringCursorIntoViewWithBuffer() {
        val layoutResult = textLayoutResult ?: return
        val cursorPosition = textFieldValue.selection.end

        if (cursorPosition >= 0 && cursorPosition <= textFieldValue.text.length) {
            try {
                val cursorRect = layoutResult.getCursorRect(cursorPosition)

                // Calculate buffer area (approximately 2 lines above and below)
                val lineHeight = cursorRect.height
                val bufferHeight = lineHeight * 4 // 2 lines of buffer

                // Create expanded rect with buffer
                val expandedRect = Rect(
                    left = cursorRect.left,
                    top = maxOf(0f, cursorRect.top - bufferHeight), // Buffer above
                    right = cursorRect.right,
                    bottom = cursorRect.bottom + bufferHeight // Buffer below
                )

                coroutineScope.launch {
                    bringIntoViewRequester.bringIntoView(expandedRect)
                }
            } catch (e: Exception) {
                // Fallback to default behavior
                coroutineScope.launch {
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
    }

    // Auto-scroll when text changes in edit mode
    LaunchedEffect(textFieldValue.text, textFieldValue.selection, isKeyboardOpen, isFocused) {
        if (isEditMode && isFocused && isKeyboardOpen) {
            delay(100) // Let layout settle
            bringCursorIntoViewWithBuffer()
        }
    }

    // Function to save and check for empty folders
    fun saveAndCheckNavigation() {
        if (diaryRepository.writeEntryContent(file, textFieldValue.text)) {
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()

            // Check if current folder becomes empty after editing
            coroutineScope.launch {
                delay(100)

                if (currentPath.isNotEmpty()) {
                    val updatedContents = diaryRepository.getFolderContents(currentPath)

                    if (updatedContents.isEmpty()) {
                        val nearestParent = (diaryRepository as DiaryRepositoryImpl)
                            .findNearestParentWithContent(currentPath)
                        onPathChanged(nearestParent)
                    }
                }
            }
        }
    }

    // Save on system back while editing
    BackHandler(enabled = isEditMode) {
        if (diaryRepository.writeEntryContent(file, textFieldValue.text)) {
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()

            coroutineScope.launch {
                delay(100)

                if (currentPath.isNotEmpty()) {
                    val updatedContents = diaryRepository.getFolderContents(currentPath)

                    if (updatedContents.isEmpty()) {
                        val nearestParent = (diaryRepository as DiaryRepositoryImpl)
                            .findNearestParentWithContent(currentPath)
                        onPathChanged(nearestParent)
                    }
                }

                isEditMode = false
                onNavigateUp()
            }
        } else {
            onNavigateUp()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isEditMode) {
                        saveAndCheckNavigation()
                    }
                    isEditMode = !isEditMode
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                    contentDescription = if (isEditMode) "Save" else "Edit"
                )
            }
        }
    ) { paddingValues ->
        if (isEditMode) {
            // EDIT MODE: Use BringIntoViewRequester without conflicting scroll modifiers
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding() // This handles keyboard padding
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue

                        // Trigger auto-scroll when text changes
                        if (isFocused && isKeyboardOpen) {
                            coroutineScope.launch {
                                delay(50)
                                bringCursorIntoViewWithBuffer()
                            }
                        }
                    },
                    onTextLayout = { layoutResult ->
                        textLayoutResult = layoutResult
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(
                        if (isDarkTheme) {
                            Color.White // White cursor for dark theme
                        } else {
                            MaterialTheme.colorScheme.primary // This gives you the grey from your theme
                        }
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .bringIntoViewRequester(bringIntoViewRequester) // Auto-scroll for cursor
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (focusState.isFocused && isKeyboardOpen) {
                                coroutineScope.launch {
                                    delay(50)
                                    bringCursorIntoViewWithBuffer()
                                }
                            }
                        },
                    decorationBox = { innerTextField ->
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = textFieldValue.text,
                            innerTextField = innerTextField,
                            enabled = true,
                            singleLine = false,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = remember { MutableInteractionSource() },
                            label = { Text("Write your thoughts...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            contentPadding = OutlinedTextFieldDefaults.contentPadding(),
                            container = {
                                OutlinedTextFieldDefaults.Container(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = remember { MutableInteractionSource() },
                                    colors = OutlinedTextFieldDefaults.colors()
                                )
                            }
                        )
                    }
                )
            }
        } else {
            // VIEW MODE: Use manual scroll for reading
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState) // Manual scroll for view mode only
            ) {
                Text(
                    text = textFieldValue.text,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun rememberKeyboardState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}
