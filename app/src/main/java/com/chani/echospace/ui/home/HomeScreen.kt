package com.chani.echospace.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chani.echospace.domain.model.DiaryEntry
import com.chani.echospace.domain.repository.VirtualFolder
import com.chani.echospace.domain.repository.VirtualListing
import com.chani.echospace.ui.components.*
import com.chani.echospace.ui.components.CreateEntryDialog
import com.chani.echospace.ui.components.CustomScrollbar
import com.chani.echospace.ui.components.FolderAccessLostCard
import com.chani.echospace.ui.components.RenameEntryDialog
import com.chani.echospace.ui.components.SlimTopBar
import com.chani.echospace.ui.components.SwipeableDiaryListItem
import com.chani.echospace.ui.editor.EditorActivity
import com.chani.echospace.ui.theme.ThemeViewModel
import com.chani.echospace.ui.settings.SettingsActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

    // Collect UI state for folder access management
    val uiState by viewModel.uiState.collectAsState()

    // refresh state
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshResult by viewModel.refreshResult.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    val isLoadingEntries by viewModel.isLoadingEntries.collectAsState()

    // Search state
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchLoading by viewModel.isSearchLoading.collectAsState()

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var revealedEntryId by remember { mutableStateOf<String?>(null) }

    // Correctly define state holders for the dialogs with explicit types
    var entryToRename by remember { mutableStateOf<DiaryEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }

    val renameError by viewModel.renameError.collectAsState()
    val createError by viewModel.createError.collectAsState()

    // Add virtual folder state collections
    val virtualListing by viewModel.virtualListing.collectAsState()
    val isVirtualMode by viewModel.isVirtualMode.collectAsState()
    val currentFolderName by viewModel.currentFolderName.collectAsState()
    // for future breadcrumb in the slimtopbar
    val currentVirtualPath by viewModel.currentVirtualPath.collectAsState()

    val editorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onEditorReturn()
    }

    // Add folder picker launcher for reselection
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        }
    }

    // Handle refresh result with snackbar
    LaunchedEffect(refreshResult) {
        refreshResult?.let { result ->
            when (result) {
                is RefreshResult.Success -> {
                    val message = if (result.newFiles > 0) {
                        "${result.totalFiles} files refreshed (${result.newFiles} new)"
                    } else {
                        "${result.totalFiles} files refreshed"
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
                is RefreshResult.Error -> {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

//    // Handle search mode back navigation
//    BackHandler(enabled = isSearching) {
//        viewModel.setSearching(false)
//    }
//
//    // Handle system back button - close swipe if revealed
//    BackHandler(enabled = revealedEntryId != null) {
//        revealedEntryId = null
//    }
//
//    // Handle back navigation for virtual folders
//    BackHandler(enabled = isVirtualMode && currentFolderName != "EchoSpace") {
//        viewModel.navigateUp()
//    }

    // Unified back handler with proper priority ordering
    BackHandler(enabled = true) {
        when {
            // Priority 1: Close search mode
            isSearching -> {
                viewModel.setSearching(false)
            }

            // Priority 2: Close swipe reveal (highest priority for user experience)
            revealedEntryId != null -> {
                revealedEntryId = null
            }

            // Priority 3: Navigate up virtual folders
            isVirtualMode && currentFolderName != "EchoSpace" -> {
                viewModel.navigateUp()
            }

            // Priority 4: Let system handle app exit
            else -> {
                // Do nothing - let the system handle back press (exit app)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadEntries()
    }

    Scaffold(
        topBar = {
            SlimTopBar(
                title = currentFolderName,
                isDarkTheme = isDarkTheme,
                onThemeToggle = themeViewModel::toggleTheme,
                onSortClick = { showSortBottomSheet = true },
                onRefreshClick = {
                    // This calls the same refresh function as pull-to-refresh
                    viewModel.refreshFiles()
                },
                isRefreshing = isRefreshing,
                onSettingsClick = {
                    // Launch SettingsActivity with the same pattern as file clicking
                    val intent = Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                },
                isSearching = isSearching,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onSearchStart = { viewModel.setSearching(true) },
                onSearchEnd = { viewModel.setSearching(false) },
                isSearchLoading = isSearchLoading,
                isVirtualMode = isVirtualMode,
                currentVirtualPath = currentVirtualPath,
                onBreadcrumbClick = { index ->
                    if (index == -1) {
                        // Navigate to root
                        viewModel.navigateToRoot()
                    } else {
                        // Navigate to specific breadcrumb level
                        val targetPath = currentVirtualPath.take(index + 1)
                        viewModel.navigateToPath(targetPath)
                    }
                }
            )
        },
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
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
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
            // Only show FAB when in normal state
            if (uiState is HomeUiState.Normal && !isSearching) {
                FloatingActionButton(
                    onClick = {
                        revealedEntryId = null
                        showCreateDialog = true
                    },
                    // Add colors parameter for complete control
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = "New File"
                    )
                }
            }
        }
    ) { padding ->
        // Check UI state and show appropriate content
        when (uiState) {
            is HomeUiState.FolderAccessLost -> {
                // Show folder access lost UI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    FolderAccessLostCard(
                        onReselectFolder = {
                            folderPickerLauncher.launch(null)
                        }
                    )
                }
            }
            is HomeUiState.Normal -> {
                if (isLoadingEntries) {
                    // Show loading indicator while files are being loaded
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            strokeWidth = 3.5.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Loading Your Files",
                            //style = MaterialTheme.typography.headlineMedium,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                        )
                    }
                } else {
                    // Show search results or normal file list
                    val (displayEntries, snippetMap) = if (isSearching) {
                        val searchResultEntries = searchResults.mapNotNull { searchResult ->
                            entries.find { it.file.uri.toString() == searchResult.uri }
                        }
                        // wrapped around remember to reduce recomposition
                        val snippetMap = remember(searchResults) {
                            searchResults.associate { it.uri to it.snippet }
                        }
                        searchResultEntries to snippetMap
                    } else {
                        entries to emptyMap()
                    }

                    // Create Box to contain both PullToRefreshBox and CustomScrollbar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {

                        // Remember LazyListState for scrollbar tracking
                        val lazyListState = rememberLazyListState()

                        // Only enable pull-to-refresh when not searching
                        if (!isSearching) {
                            // Wrap LazyColumn with PullToRefreshBox
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    // Dismiss any revealed swipe actions
                                    revealedEntryId = null
                                    viewModel.refreshFiles()
                                },
                                modifier = Modifier.fillMaxSize(),
                                state = pullToRefreshState,
                                indicator = {
                                    PullToRefreshDefaults.Indicator(
                                        modifier = Modifier.align(Alignment.TopCenter),
                                        isRefreshing = isRefreshing,
                                        state = pullToRefreshState,
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            ) {
                                FileListContent(
                                    displayEntries = displayEntries,
                                    isRefreshing = isRefreshing,
                                    lazyListState = lazyListState,
                                    revealedEntryId = revealedEntryId,
                                    onRevealedEntryIdChange = { revealedEntryId = it },
                                    onItemClick = { entry ->
                                        revealedEntryId = null
                                        viewModel.startEditingFile(entry.file.uri.toString(), currentVirtualPath)
                                        val intent = Intent(context, EditorActivity::class.java).apply {
                                            putExtra("file_uri", entry.file.uri.toString())
                                            putExtra("startineditmode", false)
                                            putExtra("initialcontent", "")
                                            //putExtra("source_virtual_path", currentVirtualPath.toTypedArray())
                                        }
                                        editorLauncher.launch(intent)
                                    },
                                    onRename = { entry ->
                                        entryToRename = entry
                                        revealedEntryId = null
                                    },
                                    onDelete = { entry ->
                                        entryToDelete = entry
                                        revealedEntryId = null
                                    },
                                    isInSearching = false,
                                    searchQuery = "",
                                    snippetMap = snippetMap,
                                    onFolderClick = { folder ->
                                        viewModel.navigateToFolder(folder)
                                    },
                                    isVirtualMode = true,
                                    virtualListing = virtualListing
                                    //currentVirtualPath = currentVirtualPath
                                )
                            }
                        } else {
                            // Search mode - no pull to refresh
                            FileListContent(
                                displayEntries = displayEntries,
                                isRefreshing = false,
                                lazyListState = lazyListState,
                                revealedEntryId = revealedEntryId,
                                onRevealedEntryIdChange = { revealedEntryId = it },
                                onItemClick = { entry ->
                                    revealedEntryId = null
                                    val intent = Intent(context, EditorActivity::class.java).apply {
                                        putExtra("file_uri", entry.file.uri.toString())
                                    }
                                    editorLauncher.launch(intent)
                                },
                                onRename = { entry ->
                                    entryToRename = entry
                                    revealedEntryId = null
                                },
                                onDelete = { entry ->
                                    entryToDelete = entry
                                    revealedEntryId = null
                                },
                                isInSearching = true,
                                searchQuery = searchQuery,
                                snippetMap = snippetMap,
                                onFolderClick = { folder ->
                                    viewModel.navigateToFolder(folder)
                                },
                                isVirtualMode = false,
                                virtualListing = null
                                //currentVirtualPath = currentVirtualPath
                            )
                        }

                        // Add CustomScrollbar overlay
                        CustomScrollbar(
                            lazyListState = lazyListState, // Pass LazyListState
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .fillMaxHeight()
                                .padding(top = 0.dp, end = 2.dp, bottom = 8.dp),
                            thickness = 4.dp,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // --- Dialog Implementations ---

    // Sort Bottom Sheet
    if (showSortBottomSheet) {
        SortBottomSheet(
            isDarkTheme = isDarkTheme,
            currentSortOption = sortOption,
            onSortOptionSelected = { newSortOption ->
                viewModel.setSortOption(newSortOption)
            },
            onDismiss = { showSortBottomSheet = false }
        )
    }

    if (showCreateDialog) {
        CreateEntryDialog(
            onDismiss = {
                showCreateDialog = false
                viewModel.clearCreateError()
            },
            onConfirm = { fileName ->
                coroutineScope.launch {
                    val file = viewModel.createEntry(fileName)
                    if (file != null) {
                        showCreateDialog = false
                        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val timestamp = timeFormatter.format(Date())
                        //val initialContent = "# $fileName $timestamp\n\n"
                        val initialContent = buildInitialContent(
                            fileName = fileName,
                            timestamp = timestamp,
                            currentVirtualPath = currentVirtualPath,
                            isVirtualMode = isVirtualMode
                        )

                        val intent = Intent(context, EditorActivity::class.java).apply {
                            putExtra("file_uri", file.uri.toString())
                            putExtra("start_in_edit_mode", true)
                            putExtra("initial_content", initialContent)
                        }
                        editorLauncher.launch(intent)
                    }
                }
            },
            errorMessage = createError,
            onErrorClear = viewModel::clearCreateError
        )
    }

    entryToRename?.let { entry ->
        RenameEntryDialog(
            currentName = entry.name,
            onDismiss = {
                entryToRename = null
                viewModel.clearRenameError() // Clear error when dialog is dismissed
            },
            onConfirm = { newName ->
                // Pass a success callback that closes the dialog
                viewModel.renameEntry(entry, newName) {
                    entryToRename = null // Close dialog only on successful rename
                }
            },
            errorMessage = renameError, // Pass the error message to the dialog
            onErrorClear = { viewModel.clearRenameError() } // Clear error when user types
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete '${entry.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        entryToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun buildInitialContent(
    fileName: String,
    timestamp: String,
    currentVirtualPath: List<String>,
    isVirtualMode: Boolean
): String {
    // Build the hashtag chain if in virtual folder mode
    val hashtagChain = if (isVirtualMode && currentVirtualPath.isNotEmpty()) {
        val hashtags = "#${currentVirtualPath.joinToString("#")}"
        "\n\n\n$hashtags"
    } else {
        ""
    }

    // Format: header + cursor position + hashtags
    return "# $fileName $timestamp\n\n<CURSOR_POSITION>$hashtagChain"
}


@Composable
private fun FileListContent(
    displayEntries: List<DiaryEntry>,
    isRefreshing: Boolean,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    revealedEntryId: String?,
    onRevealedEntryIdChange: (String?) -> Unit,
    onItemClick: (DiaryEntry) -> Unit,
    onRename: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit,
    isInSearching: Boolean = false,
    searchQuery: String = "",
    //searchResults: List<SearchResultItem> = emptyList(),
    snippetMap: Map<String, String> = emptyMap(),
    onFolderClick: (VirtualFolder) -> Unit = {},
    isVirtualMode: Boolean,
    virtualListing: VirtualListing?
    //currentVirtualPath: List<String> = emptyList()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        userScrollEnabled = !isRefreshing, // Disable user scrolling while refreshing
        overscrollEffect = null,
        state = lazyListState
    ) {
        if (displayEntries.isEmpty()) {
            item {
                // Only show content when NOT in search mode OR when search has no results
                if (!isInSearching) {
                    // Normal mode - no files exist
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Welcome to EchoSpace!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (searchQuery.isNotEmpty()) {
                    // Search mode with query but no results
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .alpha(0.3f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No items match your search",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(0.6f), // Reduced alpha
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // NEW: Show virtual folders and files when in virtual mode
            if (isVirtualMode && virtualListing != null && !isInSearching) {
                // Show folders first
                items(
                    items = virtualListing.folders,
                    key = { folder -> "folder_${folder.fullPathKeys.joinToString("/")}" },
                    contentType = { "virtual_folder" }
                ) { folder ->
                    FolderListItem(
                        folder = folder,
                        onClick = {
                            onRevealedEntryIdChange(null)
                            onFolderClick(folder)
                            // Navigate to folder using ViewModel method
                            // This will need to be passed as a parameter to FileListContent
                        }
                    )
                }

                // Then show files at current virtual location
                items(
                    items = virtualListing.files,
                    key = { it.file.uri },
                    contentType = { "diary_entry" }
                ) { entry ->
                    val isRevealed = revealedEntryId == entry.file.uri.toString()
                    val snippet = snippetMap[entry.file.uri.toString()]

                    SwipeableDiaryListItem(
                        entry = entry,
                        onItemClick = {
                            onRevealedEntryIdChange(null)
                            onItemClick(entry)
                        },
                        onRename = { onRename(entry) },
                        onDelete = { onDelete(entry) },
                        isRevealed = isRevealed,
                        onRevealChange = { shouldReveal ->
                            onRevealedEntryIdChange(
                                if (shouldReveal) entry.file.uri.toString() else null
                            )
                        },
                        isSwipeEnabled = !isRefreshing,
                        snippet = snippet,
                        isInSearchMode = false,
                        searchQuery = "",
                        isFolder = false
                    )
                }
            } else {
                // Your current items implementation for flat/search mode
                items(
                    items = displayEntries,
                    key = { it.file.uri },
                    contentType = { "diary_entry" }
                ) { entry ->
                    val isRevealed = revealedEntryId == entry.file.uri.toString()
                    val snippet = snippetMap[entry.file.uri.toString()]

                    SwipeableDiaryListItem(
                        // Keep all your existing parameters exactly as they are
                        entry = entry,
                        onItemClick = {
                            onRevealedEntryIdChange(null)
                            onItemClick(entry)
                        },
                        onRename = { onRename(entry) },
                        onDelete = { onDelete(entry) },
                        isRevealed = isRevealed,
                        onRevealChange = { shouldReveal ->
                            onRevealedEntryIdChange(
                                if (shouldReveal) entry.file.uri.toString() else null
                            )
                        },
                        isSwipeEnabled = !isRefreshing,
                        snippet = snippet,
                        isInSearchMode = isInSearching,
                        searchQuery = searchQuery,
                        isFolder = false // Add this parameter
                    )
                }
            }
        }
    }
}

@Composable
fun FolderListItem(
    folder: VirtualFolder,
    onClick: () -> Unit
) {
    SwipeableDiaryListItem(
        entry = null, // Pass null for folder items
        onItemClick = onClick,
        onRename = { /* No-op for folders */ },
        onDelete = { /* No-op for folders */ },
        isRevealed = false,
        onRevealChange = { /* No-op for folders */ },
        isSwipeEnabled = false, // Disable swipe for folders
        snippet = null,
        isInSearchMode = false,
        searchQuery = "",
        isFolder = true, // NEW: Add this parameter to distinguish folders
        folderData = folder // NEW: Pass folder data
    )
}
