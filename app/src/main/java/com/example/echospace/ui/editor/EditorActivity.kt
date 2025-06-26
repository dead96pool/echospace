// /app/src/main/java/com/example/echospace/ui/editor/EditorActivity.kt

package com.example.echospace.ui.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.echospace.ui.theme.EchoSpaceTheme
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
            EchoSpaceTheme {
                EditorScreen(
                    file = file,
                    viewModel = hiltViewModel(),
                    onNavigateUp = { finish() }
                )
            }
        }
    }
}
