// /app/src/main/java/com/example/echospace/ui/editor/EditorActivity.kt

package com.chani.echospace.ui.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.chani.echospace.ui.theme.EchoSpaceTheme
import com.chani.echospace.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fileUriString = intent.getStringExtra("file_uri")
        val startInEditMode = intent.getBooleanExtra("start_in_edit_mode", false)
        val initialContent = intent.getStringExtra("initial_content")

        if (fileUriString == null) {
            finish()
            return
        }

        val file = DocumentFile.fromSingleUri(this, fileUriString.toUri())
        if (file == null || !file.exists()) {
            finish()
            return
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

            EchoSpaceTheme(darkTheme = isDarkTheme) {
                EditorScreen(
                    file = file,
                    viewModel = hiltViewModel(),
                    startInEditMode = startInEditMode,
                    initialContent = initialContent
                )
            }
        }
    }
}
