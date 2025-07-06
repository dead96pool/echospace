// /app/src/main/java/com/example/echospace/ui/home/HomeScreen.kt

package com.example.echospace.ui.home

import android.content.Intent
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
import com.example.echospace.ui.components.CreateEntryDialog
import com.example.echospace.ui.components.DiaryListItem
import com.example.echospace.ui.components.SlimTopBar
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
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // handling the file exist error
    val createError by viewModel.createError.collectAsState()

    val editorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.loadEntries() // Refresh the list when returning from editor
    }

    LaunchedEffect(Unit) {
        viewModel.loadEntries()
    }

    Scaffold(
        topBar = {
            SlimTopBar(
                title = "EchoSpace",
                isDarkTheme = isDarkTheme,
                onThemeToggle = { themeViewModel.toggleTheme() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New File")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(entries) { entry ->
                // Replace the old Card-based item with the new clean list item
                DiaryListItem(entry = entry) {
                    val intent = Intent(context, EditorActivity::class.java).apply {
                        putExtra("file_uri", entry.file.uri.toString())
                    }
                    editorLauncher.launch(intent)
                }
                // Optional: Add a subtle divider between items
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }

    if (showDialog) {
        CreateEntryDialog(
            onDismiss = {
                showDialog = false
                viewModel.clearCreateError()
            },
            onConfirm = { fileName ->
                coroutineScope.launch {
                    val file = viewModel.createEntry(fileName)
                    // Only close the dialog if the file was created successfully
                    if (file != null) {
                        showDialog = false

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
            errorMessage = createError // Pass the error message to the dialog
        )
    }
}
