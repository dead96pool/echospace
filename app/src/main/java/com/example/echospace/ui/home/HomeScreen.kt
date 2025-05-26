package com.example.echospace.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.repository.DiaryRepository
import com.example.echospace.ui.components.CreateEntryDialog
import com.example.echospace.ui.components.DeleteConfirmationDialog
import com.example.echospace.ui.components.FolderItemComponent
import com.example.echospace.ui.components.RenameEntryDialog
import com.example.echospace.ui.editor.EditorActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(diaryRepository: DiaryRepository) {
    val context = LocalContext.current

    var currentPath by remember { mutableStateOf("") }
    // Intercept system back when in a subfolder
    BackHandler(enabled = currentPath.isNotEmpty()) {
        currentPath = diaryRepository.getParentPath(currentPath) ?: ""
    }
    var showCreateDialog by remember { mutableStateOf(false) }
    var entryToRename by remember { mutableStateOf<DiaryEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

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
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry", modifier = Modifier.size(24.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

        // Delete dialog
        entryToDelete?.let { entry ->
            DeleteConfirmationDialog(
                entry = entry,
                onDismiss = { entryToDelete = null },
                onConfirm = { e ->
                    if (diaryRepository.deleteEntry(e.file)) {
                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                        refreshTrigger++
                    } else {
                        Toast.makeText(context, "Failed to delete entry", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
