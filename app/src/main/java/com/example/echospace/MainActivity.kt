package com.example.echospace

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.echospace.data.local.FileManager
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.ui.home.HomeScreen
import com.example.echospace.ui.theme.EchoSpaceTheme
import com.example.echospace.utils.ThemePreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences
    private lateinit var fileManager: FileManager

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            fileManager.saveFolderUri(it)
            // Continue with normal app initialization
            initializeApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        themePreferences = ThemePreferences(this)
        fileManager = FileManager(this)

        // Check if user has selected a folder
        if (!fileManager.isFolderSelected()) {
            showFolderSelectionDialog()
        } else {
            initializeApp()
        }
    }

    private fun showFolderSelectionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select EchoSpace Folder")
            .setMessage("Please select where you want to store your diary files. We recommend creating an 'EchoSpace' folder in Documents.")
            .setPositiveButton("Select Folder") { _, _ ->
                folderPickerLauncher.launch(null)
            }
            .setCancelable(false)
            .show()
    }

    private fun initializeApp() {

        // Initialize repository
        val diaryRepository = DiaryRepositoryImpl(fileManager)

        setContent {
            // Collect theme state from preferences (defaults to light theme)
            val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)

            LaunchedEffect(isDarkTheme) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                // Set status bar icons to light (white) in dark theme, dark (black) in light theme
                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                // Set navigation bar icons to light (white) in dark theme, dark (black) in light theme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }

            // Use manual theme control, ignoring system theme
            EchoSpaceTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        diaryRepository = diaryRepository,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            lifecycleScope.launch {
                                themePreferences.setDarkTheme(!isDarkTheme)
                            }
                        }
                    )
                }
            }
        }
    }
}
