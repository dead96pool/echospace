// /app/src/main/java/com/example/echospace/ui/editor/EditorViewModel.kt

package com.chani.echospace.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.chani.echospace.data.local.FileOperationResult
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.echospace.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SaveStatus {
    data object Idle : SaveStatus()
    data object Saving : SaveStatus()
    data object Success : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    // Text content state for surviving configuration changes
    private val _textFieldValue = MutableStateFlow(TextFieldValue(""))
    val textFieldValue: StateFlow<TextFieldValue> = _textFieldValue.asStateFlow()

    // SaveStatus system (kept as-is)
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus = _saveStatus.asStateFlow()

    // Current file tracking
    private var currentFile: DocumentFile? = null

    // Edit mode state management
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // Track initialization and content loading
    private var isEditModeInitialized = false
    private var isContentLoaded = false

    // Loading state for content
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Initialize edit mode (with guard)
    fun initializeEditMode(startInEditMode: Boolean) {
        if (!isEditModeInitialized) {
            _isEditMode.value = startInEditMode
            isEditModeInitialized = true
        }
    }

    // Set edit mode
    fun setEditMode(editMode: Boolean) {
        _isEditMode.value = editMode
    }

    fun markContentLoaded() {
        isContentLoaded = true
    }

    // loadContent function to update both states + prevent reloading
    fun loadContent(file: DocumentFile) {
        if (isContentLoaded) return // NEW: Prevent reloading on configuration changes

        viewModelScope.launch {
            // Set the current file when loading
            currentFile = file
            val fileContent = diaryRepository.readContent(file) // Your existing direct call

            // Update both states
            _content.value = fileContent
            _textFieldValue.value = TextFieldValue(
                text = fileContent,
                selection = TextRange(fileContent.length)
            )
            isContentLoaded = true
        }
    }

    // saveContent function to use textFieldValue but keep all parameters
    fun saveContent(
        file: DocumentFile? = currentFile,
        text: String? = _textFieldValue.value.text, // NEW: Use textFieldValue text by default
        onSaveComplete: (() -> Unit)? = null // PRESERVED: Your callback parameter
    ) {

        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving

            if (file == null || text == null) {
                _saveStatus.value = SaveStatus.Error("File or content is null")
                delay(3000)
                _saveStatus.value = SaveStatus.Idle
                return@launch
            }

            when (val result = diaryRepository.saveEntry(file, text)) {
                is FileOperationResult.Success -> {
                    // Update both UI states with the new content
                    _content.value = text
                    _textFieldValue.value = TextFieldValue(
                        text = text,
                        selection = TextRange(text.length)
                    )
                    _saveStatus.value = SaveStatus.Success
                    // PRESERVED: Call the completion callback
                    onSaveComplete?.invoke()
                    delay(2000)
                    _saveStatus.value = SaveStatus.Idle
                }
                is FileOperationResult.Error -> {
                    _saveStatus.value = SaveStatus.Error(result.message)
                    delay(3000)
                    _saveStatus.value = SaveStatus.Idle
                }
            }
        }
    }

    // Update text content (for TextField onValueChange)
    fun updateTextContent(newTextFieldValue: TextFieldValue) {
        _textFieldValue.value = newTextFieldValue
        // Also update the legacy content state for compatibility
        _content.value = newTextFieldValue.text
    }
}
