// /app/src/main/java/com/chani/echospace/MainActivity.kt

package com.chani.echospace

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.chani.echospace.data.local.FileManager
import com.chani.echospace.ui.home.HomeScreen
import com.chani.echospace.ui.theme.EchoSpaceTheme
import com.chani.echospace.ui.theme.ThemeViewModel
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
                val themeViewModel: ThemeViewModel = hiltViewModel()
                val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

                EchoSpaceTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        HomeScreen(
                            viewModel = hiltViewModel(),
                            themeViewModel = themeViewModel
                        )
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
