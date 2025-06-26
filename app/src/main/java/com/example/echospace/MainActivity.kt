// /app/src/main/java/com/example/echospace/MainActivity.kt

package com.example.echospace

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.echospace.data.local.FileManager
import com.example.echospace.ui.home.HomeScreen
import com.example.echospace.ui.theme.EchoSpaceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var fileManager: FileManager

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            fileManager.saveFolderUri(it)
            recreate() // Simple way to relaunch the app with the selected folder
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!fileManager.isFolderSelected()) {
            showFolderSelectionDialog()
        } else {
            setContent {
                EchoSpaceTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        HomeScreen(viewModel = hiltViewModel())
                    }
                }
            }
        }
    }

    private fun showFolderSelectionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select EchoSpace Folder")
            .setMessage("Please select a folder to store your notes.")
            .setPositiveButton("Select") { _, _ -> folderPickerLauncher.launch(null) }
            .setCancelable(false)
            .show()
    }
}
