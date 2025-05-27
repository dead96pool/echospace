package com.example.echospace.ui.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.echospace.data.local.FileManager
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.ui.theme.EchoSpaceTheme
import com.example.echospace.utils.ThemePreferences
import kotlinx.coroutines.launch
import java.io.File

class EditorActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themePreferences = ThemePreferences(this)

        val filePath = intent.getStringExtra("file_path") ?: ""
        val startInEditMode = intent.getBooleanExtra("edit_mode", false)

        if (filePath.isEmpty()) {
            finish()
            return
        }

        val file = File(filePath)

        // Initialize repository
        val fileManager = FileManager(this)
        val diaryRepository = DiaryRepositoryImpl(fileManager)

        setContent {
            // Collect theme state from preferences
            val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)

            EchoSpaceTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(
                        file = file,
                        diaryRepository = diaryRepository,
                        startInEditMode = startInEditMode,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            lifecycleScope.launch {
                                themePreferences.setDarkTheme(!isDarkTheme)
                            }
                        },
                        onNavigateUp = { finish() }
                    )
                }
            }
        }
    }
}
