package com.example.echospace.ui.editor

import android.content.Intent
//import android.net.Uri
import androidx.core.net.toUri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.example.echospace.data.local.FileManager
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.ui.theme.EchoSpaceTheme
import com.example.echospace.utils.ThemePreferences
import kotlinx.coroutines.launch
//import java.io.File

class EditorActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Configure window for proper keyboard handling
        WindowCompat.setDecorFitsSystemWindows(window, false)

        themePreferences = ThemePreferences(this)

        val fileUriString = intent.getStringExtra("file_uri") ?: ""
        val startInEditMode = intent.getBooleanExtra("edit_mode", false)
        val currentPath = intent.getStringExtra("current_path") ?: ""

        if (fileUriString.isEmpty()) {
            finish()
            return
        }

        val fileUri = fileUriString.toUri()
        val documentFile = DocumentFile.fromSingleUri(this, fileUri)

        if (documentFile == null) {
            finish()
            return
        }


        // Initialize repository
        val fileManager = FileManager(this)
        val diaryRepository = DiaryRepositoryImpl(fileManager)

        setContent {
            // Collect theme state from preferences
            val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)

            LaunchedEffect(isDarkTheme) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }

            EchoSpaceTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(
                        file = documentFile,
                        diaryRepository = diaryRepository,
                        startInEditMode = startInEditMode,
                        isDarkTheme = isDarkTheme,
                        currentPath = currentPath,
                        onThemeToggle = {
                            lifecycleScope.launch {
                                themePreferences.setDarkTheme(!isDarkTheme)
                            }
                        },
                        onPathChanged = { newPath ->
                            // Return the new path to the calling activity
                            val resultIntent = Intent().apply {
                                putExtra("new_path", newPath)
                            }
                            setResult(RESULT_OK, resultIntent)
                        },
                        onNavigateUp = { finish() }
                    )
                }
            }
        }
    }
}
