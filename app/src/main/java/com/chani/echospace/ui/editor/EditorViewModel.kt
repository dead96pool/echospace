// /app/src/main/java/com/example/echospace/ui/editor/EditorViewModel.kt

package com.chani.echospace.ui.editor

import com.chani.echospace.data.local.FileOperationResult
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.echospace.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)

    private var currentFile: DocumentFile? = null

    // This function will save the initial content to the file
    suspend fun setInitialContentAndSave(file: DocumentFile, initialText: String) {
        currentFile = file
        _saveStatus.value = SaveStatus.Saving

        when (val result = diaryRepository.saveEntry(file, initialText)) {
            is FileOperationResult.Success -> {
                _content.value = initialText
                _saveStatus.value = SaveStatus.Success
                // Brief delay then reset
                delay(1000)
                _saveStatus.value = SaveStatus.Idle
            }
            is FileOperationResult.Error -> {
                _saveStatus.value = SaveStatus.Error(result.message)
                delay(3000)
                _saveStatus.value = SaveStatus.Idle
            }
        }
    }

    fun loadContent(file: DocumentFile) {
        viewModelScope.launch {
            // Set the current file when loading
            currentFile = file
            _content.value = diaryRepository.readContent(file)
        }
    }

    fun saveContent(
        file: DocumentFile? = currentFile,
        text: String? = _content.value,
        onSaveComplete: (() -> Unit)? = null
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
                    // Update the UI state with the new content (your existing functionality)
                    _content.value = text
                    _saveStatus.value = SaveStatus.Success
                    // Call the completion callback (your existing functionality)
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

//    fun updateContent(newContent: String) {
//        _content.value = newContent
//    }
}
