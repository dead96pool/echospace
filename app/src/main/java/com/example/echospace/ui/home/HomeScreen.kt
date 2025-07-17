package com.example.echospace.ui.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

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
                onThemeToggle = { themeViewModel.toggleTheme() },
                onSortClick = { showSortBottomSheet = true }
            )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                        revealedEntryId = null // Hide after action
                    },
                    onDelete = {
                        entryToDelete = entry
                        revealedEntryId = null // Hide after action
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

    // --- Dialog Implementations ---

    // Sort Bottom Sheet
    if (showSortBottomSheet) {
        SortBottomSheet(
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
            errorMessage = createError
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
