// /app/src/main/java/com/chani/echospace/ui/home/HomeViewModel.kt

package com.chani.echospace.ui.home

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.echospace.data.local.FileAlreadyExistsException
import com.chani.echospace.data.local.FolderAccessLostException
import com.chani.echospace.data.preferences.ThemePreferences
import com.chani.echospace.domain.model.DiaryEntry
import com.chani.echospace.domain.model.SortOption
import com.chani.echospace.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

sealed class RefreshResult {
    data class Success(val totalFiles: Int, val newFiles: Int) : RefreshResult()
    data class Error(val message: String) : RefreshResult()
}
// UI state for folder access management
sealed class HomeUiState {
    data object Normal : HomeUiState()
    data object FolderAccessLost : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries.asStateFlow()

    private val _renameError = MutableStateFlow<String?>(null)
    val renameError: StateFlow<String?> = _renameError.asStateFlow()

    // Add state for creation errors
    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _refreshResult = MutableStateFlow<RefreshResult?>(null)
    val refreshResult: StateFlow<RefreshResult?> = _refreshResult.asStateFlow()

    // NEW: Add loading state for data fetching
    private val _isLoadingEntries = MutableStateFlow(false)
    val isLoadingEntries: StateFlow<Boolean> = _isLoadingEntries.asStateFlow()

    val sortOption: StateFlow<SortOption> = themePreferences.sortOption
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SortOption.DATE_NEWEST
        )


    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Normal)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Load entries whenever sort option changes
        viewModelScope.launch {
            sortOption.collect { sortOption ->
                loadEntries(sortOption)
            }
        }
    }

    // updated for folder access management
    fun loadEntries(sortOption: SortOption? = null) {
        viewModelScope.launch {
            try {
                _isLoadingEntries.value = true // Start loading
                _uiState.value = HomeUiState.Normal // Reset to normal state
                val currentSort = sortOption ?: this@HomeViewModel.sortOption.value
                _entries.value = diaryRepository.getEntries(currentSort)
            } catch (e: FolderAccessLostException) {
                // Handle folder access loss
                _uiState.value = HomeUiState.FolderAccessLost
                _entries.value = emptyList()
            } catch (e: Exception) {
                // Handle any other errors (preserves existing behavior)
                _entries.value = emptyList()
            } finally {
                _isLoadingEntries.value = false // Stop loading
            }
        }
    }

    fun setSortOption(sortOption: SortOption) {
        viewModelScope.launch {
            themePreferences.setSortOption(sortOption)
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

    /**
     * Enhanced refresh with metadata cache updates
     * Maintains all existing functionality while adding metadata refresh
     */
    fun refreshFiles() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _uiState.value = HomeUiState.Normal // Reset to normal state
                val currentSort = sortOption.value
                val previousEntries = _entries.value

                // Refresh metadata cache first (your existing logic)
                diaryRepository.refreshAllMetadata()

                // Get refreshed entries with current sort order (your existing logic)
                val refreshedEntries = diaryRepository.getEntries(currentSort)
                _entries.value = refreshedEntries

                // Calculate refresh statistics (your existing logic)
                val refreshCount = refreshedEntries.size
                val newFiles = refreshedEntries.size - previousEntries.size
                _refreshResult.value = RefreshResult.Success(refreshCount, newFiles)

                // Brief delay to show refresh indicator (your existing logic)
                delay(500)
            } catch (e: FolderAccessLostException) {
                // Handle folder access loss
                _uiState.value = HomeUiState.FolderAccessLost
                _entries.value = emptyList()
                _refreshResult.value = RefreshResult.Error("Folder access lost. Please reselect your folder.")
            } catch (e: Exception) {
                // Handle other errors (preserves existing behavior)
                _refreshResult.value = RefreshResult.Error("Failed to refresh: ${e.message}")
            } finally {
                _isRefreshing.value = false
                // Clear result after showing (your existing logic)
                delay(3000)
                _refreshResult.value = null
            }
        }
    }

//    fun clearRefreshResult() {
//        _refreshResult.value = null
//    }

    // NEW: Function to handle folder reselection
    private fun onFolderReselected() {
        _uiState.value = HomeUiState.Normal
        _isLoadingEntries.value = true // Show loading during folder recovery
        loadEntries()
    }

    // NEW: Function to handle folder selection (will be used by HomeScreen)
    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // You'll need to access your FileManager through DiaryRepository
                // We'll add this method to DiaryRepository interface in the next step
                diaryRepository.saveFolderUri(uri)
                onFolderReselected()
            } catch (e: Exception) {
                // Could add error handling here if needed
                _refreshResult.value = RefreshResult.Error("Failed to save folder selection")
            }
        }
    }

}

