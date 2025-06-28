// /app/src/main/java/com/example/echospace/ui/editor/EditorViewModel.kt

package com.example.echospace.ui.editor

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echospace.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()

    fun loadContent(file: DocumentFile) {
        viewModelScope.launch {
            _content.value = diaryRepository.readContent(file)
        }
    }

    fun saveContent(file: DocumentFile, text: String, onSaveComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            // Save to file
            diaryRepository.writeContent(file, text)

            // Update the UI state with the new content
            _content.value = text

            // Call the completion callback
            onSaveComplete?.invoke()
        }
    }
}
