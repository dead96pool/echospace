package com.example.echospace.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.ui.components.*
import com.example.echospace.ui.components.CreateEntryDialog
import com.example.echospace.ui.components.RenameEntryDialog
import com.example.echospace.ui.components.SlimTopBar
import com.example.echospace.ui.components.SwipeableDiaryListItem
import com.example.echospace.ui.editor.EditorActivity
import com.example.echospace.ui.theme.ThemeViewModel
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

    // refresh state
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshResult by viewModel.refreshResult.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
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

    val editorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.loadEntries() // Refresh the list when returning from editor
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

    // Handle system back button - close swipe if revealed
    BackHandler(enabled = revealedEntryId != null) {
        revealedEntryId = null
    }

    LaunchedEffect(Unit) {
        viewModel.loadEntries()
    }

    Scaffold(
        topBar = {
            SlimTopBar(
                title = "EchoSpace",
                isDarkTheme = isDarkTheme,
                onThemeToggle = themeViewModel::toggleTheme,
                onSortClick = { showSortBottomSheet = true }
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
            FloatingActionButton(
                onClick = {
                    revealedEntryId = null
                    showCreateDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New File")
            }
        }
    ) { padding ->
        // Wrap LazyColumn with PullToRefreshBox
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                // Dismiss any revealed swipe actions
                revealedEntryId = null
                viewModel.refreshFiles()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(entries, key = { it.file.uri }) { entry ->
                    val isRevealed = revealedEntryId == entry.file.uri.toString()

                    SwipeableDiaryListItem(
                        entry = entry,
                        onItemClick = {
                            revealedEntryId = null
                            val intent = Intent(context, EditorActivity::class.java).apply {
                                putExtra("file_uri", entry.file.uri.toString())
                            }
                            editorLauncher.launch(intent)
                        },
                        onRename = {
                            entryToRename = entry
                            revealedEntryId = null
                        },
                        onDelete = {
                            entryToDelete = entry
                            revealedEntryId = null
                        },
                        isRevealed = isRevealed,
                        onRevealChange = { shouldReveal ->
                            revealedEntryId = if (shouldReveal) entry.file.uri.toString() else null
                        }
                    )
                    //HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                        val initialContent = "# $fileName $timestamp\n\n"
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
                Button(onClick = {
                    viewModel.deleteEntry(entry)
                    entryToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
