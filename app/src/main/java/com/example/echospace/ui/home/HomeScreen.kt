// /app/src/main/java/com/example/echospace/ui/home/HomeScreen.kt

package com.example.echospace.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
//import androidx.hilt.navigation.compose.hiltViewModel
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.ui.components.CreateEntryDialog
import com.example.echospace.ui.components.SlimTopBar
import com.example.echospace.ui.editor.EditorActivity
import com.example.echospace.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

//@OptIn(ExperimentalMaterial3Api::class)
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                DiaryEntryItem(entry = entry) {
                    val intent = Intent(context, EditorActivity::class.java).apply {
                        putExtra("file_uri", entry.file.uri.toString())
                        //putExtra("is_dark_theme", isDarkTheme)
                    }
                    editorLauncher.launch(intent)
                }
            }
        }
    }

    if (showDialog) {
        CreateEntryDialog(
            onDismiss = { showDialog = false },
            onConfirm = { fileName ->
                coroutineScope.launch {
                    val file = viewModel.createEntry(fileName)
                    showDialog = false
                    file?.let {
                        val intent = Intent(context, EditorActivity::class.java).apply {
                            putExtra("file_uri", it.uri.toString())
                            //putExtra("is_dark_theme", isDarkTheme)
                        }
                        editorLauncher.launch(intent)
                    }
                }
            }
        )
    }
}

@Composable
private fun DiaryEntryItem(entry: DiaryEntry, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(text = entry.name, modifier = Modifier.padding(16.dp))
    }
}
