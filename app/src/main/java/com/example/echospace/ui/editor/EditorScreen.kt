package com.example.echospace.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.echospace.domain.repository.DiaryRepository
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    file: File,
    diaryRepository: DiaryRepository,
    startInEditMode: Boolean,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateUp: () -> Unit
) {
    var isEditMode by remember { mutableStateOf(startInEditMode) }
    var content by remember { mutableStateOf(diaryRepository.readEntryContent(file)) }
    val context = LocalContext.current

    // Scroll state for both view and edit
    val scrollState = rememberScrollState()

    // Save on system back while editing
    BackHandler(enabled = isEditMode) {
        if (diaryRepository.writeEntryContent(file, content)) {
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        }
        onNavigateUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.nameWithoutExtension) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            if (diaryRepository.writeEntryContent(file, content)) {
                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                        onNavigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Theme toggle button
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme)
                                Icons.Default.LightMode
                            else
                                Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isEditMode) {
                        if (diaryRepository.writeEntryContent(file, content)) {
                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                    isEditMode = !isEditMode
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                    contentDescription = if (isEditMode) "Save" else "Edit"
                )
            }
        }
    ) { paddingValues ->
        if (isEditMode) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                label = { Text("Write your thoughts...") }
            )
        } else {
            Text(
                text = content,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
