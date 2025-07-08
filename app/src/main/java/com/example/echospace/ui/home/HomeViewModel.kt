// /app/src/main/java/com/example/echospace/ui/home/HomeViewModel.kt

package com.example.echospace.ui.home

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echospace.data.local.FileAlreadyExistsException
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _renameError = MutableStateFlow<String?>(null)
    val renameError = _renameError.asStateFlow()

    // Add state for creation errors
    private val _createError = MutableStateFlow<String?>(null)
    val createError = _createError.asStateFlow()

    fun loadEntries() {
        viewModelScope.launch {
            _entries.value = diaryRepository.getEntries()
        }
    }

    suspend fun createEntry(fileName: String): DocumentFile? {
        return try {
            _createError.value = null // Clear previous error
            diaryRepository.createEntry(fileName)
        } catch (e: FileAlreadyExistsException) {
            _createError.value = e.message // Set error message from the exception
            null
        }
    }

    fun renameEntry(entry: DiaryEntry, newName: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _renameError.value = null // Clear previous error
                diaryRepository.renameEntry(entry, newName)
                loadEntries() // Refresh the list
                _renameError.value = null // Ensure error is cleared on success
                onSuccess() // Call success callback to close dialog
            } catch (e: FileAlreadyExistsException) {
                _renameError.value = e.message // Set error message
                // Don't call onSuccess, keep dialog open
            }
        }
    }

    // Function to clear the rename error message
    fun clearRenameError() {
        _renameError.value = null
    }

    // Function to clear the error message
    fun clearCreateError() {
        _createError.value = null
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            diaryRepository.deleteEntry(entry)
            loadEntries() // Refresh the list
        }
    }

}
