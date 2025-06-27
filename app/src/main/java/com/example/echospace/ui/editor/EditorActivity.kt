// /app/src/main/java/com/example/echospace/ui/editor/EditorActivity.kt

package com.example.echospace.ui.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.echospace.ui.theme.EchoSpaceTheme
import com.example.echospace.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fileUriString = intent.getStringExtra("file_uri")
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
                    themeViewModel = themeViewModel,
                    onNavigateUp = { finish() }
                )
            }
        }
    }
}
