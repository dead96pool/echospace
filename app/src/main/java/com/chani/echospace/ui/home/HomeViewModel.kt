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
import com.chani.echospace.data.local.MetadataUtils
import com.chani.echospace.data.preferences.ThemePreferences
import com.chani.echospace.data.repository.VirtualFolderRepository
import com.chani.echospace.domain.model.DiaryEntry
import com.chani.echospace.domain.model.SortOption
import com.chani.echospace.domain.repository.DiaryRepository
import com.chani.echospace.domain.repository.VirtualListing
import com.chani.echospace.domain.repository.VirtualFolder
import com.chani.echospace.utils.PerformanceLogger
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
import java.util.Locale
import kotlin.system.measureTimeMillis

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
    private val themePreferences: ThemePreferences,
    private val virtualFolderRepository: VirtualFolderRepository
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

    // Virtual folder navigation state
    private val _currentVirtualPath = MutableStateFlow<List<String>>(emptyList())
    val currentVirtualPath: StateFlow<List<String>> = _currentVirtualPath.asStateFlow()

    private val _virtualListing = MutableStateFlow<VirtualListing?>(null)
    val virtualListing: StateFlow<VirtualListing?> = _virtualListing.asStateFlow()

    // Track if we're using virtual folder mode or flat view
    private val _isVirtualMode = MutableStateFlow(true)
    val isVirtualMode: StateFlow<Boolean> = _isVirtualMode.asStateFlow()

    // Current folder display name for SlimTopBar
    private val _currentFolderName = MutableStateFlow("EchoSpace")
    val currentFolderName: StateFlow<String> = _currentFolderName.asStateFlow()

    // Editor state management - tracks which file is being edited
    private val _currentlyEditingFile = MutableStateFlow<String?>(null)
    val currentlyEditingFile: StateFlow<String?> = _currentlyEditingFile.asStateFlow()

    // Track when editor returns
    private val _editorReturned = MutableStateFlow<String?>(null)

    // Call when opening editor
    fun startEditingFile(fileUri: String) {
        _currentlyEditingFile.value = fileUri
    }

    // Call when editor returns
    fun onEditorReturn() {
        val editedFileUri = _currentlyEditingFile.value
        if (editedFileUri != null) {
            handleEditorReturn(editedFileUri)
            _currentlyEditingFile.value = null
        } else {
            // Fallback: just refresh
            loadEntries()
        }
    }


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

        // NEW: Sync database on app launch
        viewModelScope.launch {
            try {
                // Check if sync is needed (lightweight check first)
                val syncNeeded = diaryRepository.isDatabaseSyncNeeded()

                if (syncNeeded) {
                    Log.d("HomeViewModel", "🔄 Database sync needed on app launch")
                    diaryRepository.refreshAllMetadata() // This will trigger full sync
                } else {
                    Log.d("HomeViewModel", "✅ Database is up to date")
                }

                // Load initial entries
                loadEntries()

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to sync on app launch", e)
                // Fallback: load entries anyway
                loadEntries()
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
            // Return to virtual folder mode when search is cleared
            _isVirtualMode.value = true
            loadEntries()
        } else {
            // Switch to flat mode for global search
            _isVirtualMode.value = false
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
                val lowerQuery = query.lowercase().trim()

                // Support both phrase and individual word matching
                val searchWords = lowerQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

                // Step 1: Find filename matches
                val nameMatches = allEntries.filter { entry ->
                    val lowerName = entry.name.lowercase()
                    // Check if filename contains all search words (in any order)
                    searchWords.all { word -> lowerName.contains(word) }
                }

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

                // Step 2: Find content matches
                for (entry in allEntries) {
                    if (!nameMatches.contains(entry)) {
                        try {
                            val content = contentCache.get(entry.file.uri.toString()) ?: run {
                                val fileContent = diaryRepository.readContent(entry.file)
                                contentCache.put(entry.file.uri.toString(), fileContent)
                                fileContent
                            }

                            val lowerContent = content.lowercase()

                            // Multi-word search logic
                            val hasMatch = if (searchWords.size == 1) {
                                // Single word search
                                lowerContent.contains(searchWords[0])
                            } else {
                                // Multi-word search: check if ALL words are present (in any order)
                                searchWords.all { word -> lowerContent.contains(word) }
                            }

                            if (hasMatch) {
                                val (snippet, _) = extractSnippetWithHighlight(content, lowerQuery)
                                results.add(
                                    SearchResultItem(
                                        uri = entry.file.uri.toString(),
                                        title = entry.name,
                                        snippet = snippet,
                                        score = if (searchWords.size == 1) 0.8f else 0.9f // Higher score for multi-word matches
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w("HomeViewModel", "Failed to read content for search: ${entry.name}", e)
                        }
                    }
                }

                // Sort results by relevance score
                _searchResults.value = results.sortedByDescending { it.score }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Search failed", e)
            _searchResults.value = emptyList()
        } finally {
            _isSearchLoading.value = false
        }
    }

//    // Helper function to extract snippet around search match
//    private fun extractSnippet(content: String, query: String): String {
//        val words = content.split(Regex("\\s+"))
//        val wordsLower = words.map { it.lowercase(Locale.getDefault()) }
//        val lowerQuery = query.lowercase(Locale.getDefault())
//
//        // Find first word that contains the query
//        val matchIndex = wordsLower.indexOfFirst { word -> word.contains(lowerQuery) }
//
//        if (matchIndex == -1) {
//            return content.take(100) + if (content.length > 100) "..." else ""
//        }
//
//        val contextBefore = 3
//        val contextAfter = 4
//        val startIndex = maxOf(0, matchIndex - contextBefore)
//        val endIndex = minOf(words.size - 1, matchIndex + contextAfter)
//
//        val snippetWords = words.subList(startIndex, endIndex + 1)
//        val snippet = snippetWords.joinToString(" ")
//
//        val prefix = if (startIndex > 0) "..." else ""
//        val suffix = if (endIndex < words.size - 1) "..." else ""
//
//        return "$prefix$snippet$suffix"
//    }

    private fun extractSnippetWithHighlight(content: String, query: String): Pair<String, IntRange?> {
        val words = content.split(Regex("\\s+"))
        val wordsLower = words.map { it.lowercase(Locale.getDefault()) }
        val lowerQuery = query.lowercase(Locale.getDefault()).trim()
        val queryWords = lowerQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        // Find the best match position for multi-word queries
        val matchIndex = if (queryWords.size == 1) {
            // Single word: find first containing word
            wordsLower.indexOfFirst { word -> word.contains(queryWords[0]) }
        } else {
            // Multi-word: find first word that matches any of the query words
            wordsLower.indexOfFirst { word ->
                queryWords.any { queryWord -> word.contains(queryWord) }
            }
        }

        // Extract context around the match
        val contextBefore = 3
        val contextAfter = 4
        val startIndex = maxOf(0, matchIndex - contextBefore)
        val endIndex = minOf(words.size - 1, matchIndex + contextAfter)

        val snippetWords = words.subList(startIndex, endIndex + 1)
        val snippet = snippetWords.joinToString(" ")

        val prefix = if (startIndex > 0) "..." else ""
        val suffix = if (endIndex < words.size - 1) "..." else ""
        val finalSnippet = "$prefix$snippet$suffix"

        return finalSnippet to null
    }

    // updated for folder access management
    fun loadEntries(sortOption: SortOption? = null) {
        viewModelScope.launch {
            try {
                _isLoadingEntries.value = _entries.value.isEmpty() // Start loading only when _entries empty
                _uiState.value = HomeUiState.Normal // Reset to normal state
                val currentSort = sortOption ?: this@HomeViewModel.sortOption.value
                if (_isVirtualMode.value) {
                    // Load virtual listing (folders + files for current path)
                    val listing = diaryRepository.getVirtualListing(_currentVirtualPath.value, currentSort)
                    _virtualListing.value = listing
                    // Keep flat entries for search compatibility
                    _entries.value = diaryRepository.getEntries(currentSort)
                } else {
                    // Load flat entries (for search mode)
                    _entries.value = diaryRepository.getEntries(currentSort)
                    // Clear virtual listing when in flat mode
                    _virtualListing.value = null
                }
            } catch (e: FolderAccessLostException) {
                // Handle folder access loss
                _uiState.value = HomeUiState.FolderAccessLost
                _entries.value = emptyList()
                _virtualListing.value = null
            } catch (e: Exception) {
                // Handle any other errors (preserves existing behavior)
                _entries.value = emptyList()
                _virtualListing.value = null
            } finally {
                _isLoadingEntries.value = false // Stop loading
            }
        }
    }

    // Navigate into a virtual folder
    fun navigateToFolder(folder: VirtualFolder) {
        PerformanceLogger.logStep("navigateToFolder started: ${folder.nameDisplay}")

        viewModelScope.launch {
            val totalTime = measureTimeMillis {
                _currentVirtualPath.value = folder.fullPathKeys
                _currentFolderName.value = folder.nameDisplay

                PerformanceLogger.measureAndLog("loadEntries from navigateToFolder") {
                    loadEntries()
                }
            }

            Log.d("Navigation", "🏁 navigateToFolder total: ${totalTime}ms")
            if (totalTime > 500) {
                Log.w("Navigation", "🐌 Slow folder navigation: ${totalTime}ms")
            }
        }
    }

    // Navigate up one level (for Back button)
    fun navigateUp(): Boolean {
        PerformanceLogger.logStep("navigateUp started")

        val currentPath = _currentVirtualPath.value
        if (currentPath.isEmpty()) {
            return false
        }

        viewModelScope.launch {
            val totalTime = measureTimeMillis {
                val parentPath = currentPath.dropLast(1)
                _currentVirtualPath.value = parentPath

                _currentFolderName.value = if (parentPath.isEmpty()) {
                    "EchoSpace"
                } else {
                    parentPath.lastOrNull() ?: "EchoSpace"
                }

                PerformanceLogger.measureAndLog("loadEntries from navigateUp") {
                    loadEntries()
                }
            }

            Log.d("Navigation", "🏁 navigateUp total: ${totalTime}ms")
        }

        return true
    }

    // Navigate to root
    fun navigateToRoot() {
        _currentVirtualPath.value = emptyList()
        _currentFolderName.value = "EchoSpace"
        loadEntries()
    }

    // Handle returning from Editor (when hashtags might have changed)
    private fun handleEditorReturn(fileUri: String) {
        viewModelScope.launch {
            try {
                // Find the file that was edited
                val allEntries = diaryRepository.getEntries(sortOption.value)
                val editedEntry = allEntries.find { it.file.uri.toString() == fileUri }

                if (editedEntry != null) {
                    // Read the file content to check for new virtual paths
                    val content = diaryRepository.readContent(editedEntry.file)
                    val virtualPaths = MetadataUtils.buildVirtualPathsFromContent(content)

                    if (virtualPaths.isNotEmpty()) {
                        // Navigate to the first virtual path of the file
                        val firstPath = virtualPaths.first()
                        val pathKeys = firstPath.map { it.key }
                        _currentVirtualPath.value = pathKeys
                        _currentFolderName.value = firstPath.lastOrNull()?.display ?: "EchoSpace"
                    }
                }

                // Reload entries to reflect changes
                loadEntries()

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error handling editor return", e)
                // Fallback: just reload current view
                loadEntries()
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

