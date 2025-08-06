// /app/src/main/java/com/chani/echospace/ui/home/HomeViewModel.kt

package com.chani.echospace.ui.home

import android.net.Uri
import android.util.Log
import android.util.LruCache
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.FlowPreview
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

data class SearchResultItem(
    val uri: String,
    val title: String,
    val snippet: String,
    val score: Float
)

@OptIn(FlowPreview::class)
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

    // Add loading state for data fetching
    private val _isLoadingEntries = MutableStateFlow(false)
    val isLoadingEntries: StateFlow<Boolean> = _isLoadingEntries.asStateFlow()

    // Search mode state
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    // Search query text
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    // Search results
    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()
    // Search loading state
    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading: StateFlow<Boolean> = _isSearchLoading.asStateFlow()
    // LRU Cache for file contents (cache last 20 files)
    private val contentCache = LruCache<String, String>(20)


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

        // Search query observer with debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        _isSearchLoading.value = false
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    // ======= SEARCH FUNCTIONS =======

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearching(isSearching: Boolean) {
        _isSearching.value = isSearching
        if (!isSearching) {
            // Exit search mode - clear everything
            _searchResults.value = emptyList()
            _searchQuery.value = ""
            _isSearchLoading.value = false
        }
    }

//    fun clearSearchQuery() {
//        _searchQuery.value = ""
//    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) return

        _isSearchLoading.value = true

        try {
            withContext(Dispatchers.IO) {
                val results = mutableListOf<SearchResultItem>()
                val allEntries = entries.value
                val lowerQuery = query.lowercase()

                // Step 1: Find filename matches first
                val nameMatches = allEntries.filter { entry ->
                    entry.name.lowercase().contains(lowerQuery)
                }

                // Add filename matches as SearchResultItem objects
                nameMatches.forEach { entry ->
                    results.add(
                        SearchResultItem(
                            uri = entry.file.uri.toString(),
                            title = entry.name,
                            snippet = "Found in filename",
                            score = 1.0f
                        )
                    )
                }

                // Step 2: Find content matches (excluding name matches)
                for (entry in allEntries) {
                    if (!nameMatches.contains(entry)) {
                        try {
                            // Check cache first
                            val content = contentCache.get(entry.file.uri.toString()) ?: run {
                                // Load from disk and cache it
                                val fileContent = diaryRepository.readContent(entry.file)
                                contentCache.put(entry.file.uri.toString(), fileContent)
                                fileContent
                            }

                            if (content.lowercase().contains(lowerQuery)) {
                                // Extract snippet around the match
                                val snippet = extractSnippet(content, lowerQuery)
                                results.add(
                                    SearchResultItem(
                                        uri = entry.file.uri.toString(),
                                        title = entry.name,
                                        snippet = snippet,
                                        score = 0.8f
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w("HomeViewModel", "Failed to read content for search: ${entry.name}", e)
                        }
                    }
                }

                _searchResults.value = results
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Search failed", e)
            _searchResults.value = emptyList()
        } finally {
            _isSearchLoading.value = false
        }
    }

    // Helper function to extract snippet around search match
    private fun extractSnippet(content: String, query: String): String {
        val index = content.lowercase().indexOf(query.lowercase())
        return if (index >= 0) {
            val start = maxOf(0, index - 50)
            val end = minOf(content.length, index + query.length + 50)
            content.substring(start, end).trim()
        } else {
            content.take(100)
        }
    }

    // updated for folder access management
    fun loadEntries(sortOption: SortOption? = null) {
        viewModelScope.launch {
            try {
                _isLoadingEntries.value = _entries.value.isEmpty() // Start loading only when _entries empty
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

    // Add timestamp tracking
    private var lastRefreshTime = 0L
    private val refreshCooldown = 5000L // 5 seconds
    /**
     * Enhanced refresh with metadata cache updates
     * Maintains all existing functionality while adding metadata refresh
     */
    fun refreshFiles() {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()

            // Avoid excessive refreshes
            if (currentTime - lastRefreshTime < refreshCooldown) {
                return@launch
            }

            _isRefreshing.value = true
            try {
                _uiState.value = HomeUiState.Normal
                val currentSort = sortOption.value
                val previousEntries = _entries.value

                // Only refresh metadata of what changed
                diaryRepository.refreshAllMetadata()

                val refreshedEntries = diaryRepository.getEntries(currentSort)
                _entries.value = refreshedEntries

                val refreshCount = refreshedEntries.size
                val newFiles = refreshedEntries.size - previousEntries.size
                _refreshResult.value = RefreshResult.Success(refreshCount, newFiles)

                lastRefreshTime = currentTime
                delay(500)
            } catch (e: FolderAccessLostException) {
                _uiState.value = HomeUiState.FolderAccessLost
                _entries.value = emptyList()
                _refreshResult.value = RefreshResult.Error("Folder access lost. Please reselect your folder.")
            } catch (e: Exception) {
                _refreshResult.value = RefreshResult.Error("Failed to refresh: ${e.message}")
            } finally {
                _isRefreshing.value = false
                delay(3000)
                _refreshResult.value = null
            }
        }
    }

//    fun clearRefreshResult() {
//        _refreshResult.value = null
//    }

    // Function to handle folder reselection
    private fun onFolderReselected() {
        _uiState.value = HomeUiState.Normal
        _isLoadingEntries.value = true // Show loading during folder recovery
        loadEntries()
    }

    // Function to handle folder selection (will be used by HomeScreen)
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

