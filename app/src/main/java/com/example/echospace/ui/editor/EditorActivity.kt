// ui/editor/EditorActivity.kt
package com.example.echospace.ui.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.echospace.data.local.FileManager
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.ui.theme.EchoSpaceTheme
import java.io.File

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            EchoSpaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(
                        file = file,
                        diaryRepository = diaryRepository,
                        startInEditMode = startInEditMode,
                        onNavigateUp = { finish() }
                    )
                }
            }
        }
    }
}
