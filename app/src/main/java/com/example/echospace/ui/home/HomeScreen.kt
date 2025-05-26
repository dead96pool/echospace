package com.example.echospace.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.repository.DiaryRepository
import com.example.echospace.ui.components.CreateEntryDialog
import com.example.echospace.ui.components.DeleteConfirmationDialog
import com.example.echospace.ui.components.FolderItemComponent
import com.example.echospace.ui.components.RenameEntryDialog
import com.example.echospace.ui.editor.EditorActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(diaryRepository: DiaryRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentPath by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var entryToRename by remember { mutableStateOf<DiaryEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Intercept system back when in a subfolder
    BackHandler(enabled = currentPath.isNotEmpty()) {
        currentPath = diaryRepository.getParentPath(currentPath) ?: ""
    }

    // Recompute folderContents whenever currentPath or refreshTrigger changes
    val folderContents by remember(currentPath, refreshTrigger) {
        mutableStateOf(diaryRepository.getFolderContents(currentPath))
    }

    // Observe Activity lifecycle to refresh when returning from editor
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Pull refresh function
    fun onRefresh() {
        coroutineScope.launch {
            isRefreshing = true
            // Simulate refresh delay (you can remove this delay in production)
            delay(1000)
            // Trigger refresh by incrementing the trigger
            refreshTrigger++
            isRefreshing = false
            Toast.makeText(context, "Files refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    // Pull refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = ::onRefresh
    )

    // Helper function to navigate to nearest parent with content
    fun navigateToNearestParentWithContent() {
        val nearestParent = (diaryRepository as DiaryRepositoryImpl)
            .findNearestParentWithContent(currentPath)
        currentPath = nearestParent
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (currentPath.isEmpty()) "EchoSpace" else currentPath.replace("#", " / "))
                },
                navigationIcon = {
                    if (currentPath.isNotEmpty()) {
                        IconButton(onClick = {
                            currentPath = diaryRepository.getParentPath(currentPath) ?: ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* TODO: Theme toggle */ }) {
                        Icon(Icons.Default.LightMode, contentDescription = "Toggle Theme")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Entry",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items(folderContents) { folderItem ->
                    FolderItemComponent(
                        item = folderItem,
                        onClick = {
                            if (folderItem.isFolder) {
                                currentPath = folderItem.path
                            } else {
                                folderItem.entry?.let { entry ->
                                    val intent = Intent(context, EditorActivity::class.java)
                                        .apply { putExtra("file_path", entry.file.absolutePath) }
                                    context.startActivity(intent)
                                }
                            }
                        },
                        onRename = { it.entry?.let { entryToRename = it } },
                        onDelete = { it.entry?.let { entryToDelete = it } }
                    )
                }
            }

            // Pull refresh indicator
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }

        // Create dialog
        if (showCreateDialog) {
            CreateEntryDialog(
                onDismiss = { showCreateDialog = false },
                onFileNameConfirmed = { fileName ->
                    val file = if (fileName.isBlank())
                        diaryRepository.createEntryWithCurrentDate()
                    else
                        diaryRepository.createEntry(fileName)

                    if (file != null) {
                        context.startActivity(
                            Intent(context, EditorActivity::class.java)
                                .apply {
                                    putExtra("file_path", file.absolutePath)
                                    putExtra("edit_mode", true)
                                }
                        )
                    } else {
                        Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
                    }

                    showCreateDialog = false
                    refreshTrigger++
                }
            )
        }

        // Rename dialog
        entryToRename?.let { entry ->
            RenameEntryDialog(
                entry = entry,
                onDismiss = { entryToRename = null },
                onRename = { e, newName ->
                    if (diaryRepository.renameEntry(e.file, newName)) {
                        Toast.makeText(context, "Entry renamed", Toast.LENGTH_SHORT).show()
                        refreshTrigger++
                    } else {
                        Toast.makeText(context, "Failed to rename entry", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Delete dialog with smart navigation
        entryToDelete?.let { entry ->
            DeleteConfirmationDialog(
                entry = entry,
                onDismiss = { entryToDelete = null },
                onConfirm = { e ->
                    if (diaryRepository.deleteEntry(e.file)) {
                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                        refreshTrigger++

                        // Check if current folder still has content after deletion
                        // Use a small delay to ensure the file system has updated
                        coroutineScope.launch {
                            delay(100) // Small delay for file system consistency
                            if (!diaryRepository.hasContentInPath(currentPath)) {
                                navigateToNearestParentWithContent()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Failed to delete entry", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
