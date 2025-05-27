package com.example.echospace

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
import com.example.echospace.ui.home.HomeScreen
import com.example.echospace.ui.theme.EchoSpaceTheme
import com.example.echospace.utils.ThemePreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themePreferences = ThemePreferences(this)

        // Initialize repository
        val fileManager = FileManager(this)
        val diaryRepository = DiaryRepositoryImpl(fileManager)

        setContent {
            // Collect theme state from preferences (defaults to light theme)
            val isDarkTheme by themePreferences.isDarkTheme.collectAsState(initial = false)

            // Use manual theme control, ignoring system theme
            EchoSpaceTheme(
                darkTheme = isDarkTheme, // Use stored preference instead of system theme
                dynamicColor = false // Disable dynamic colors to use custom theme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        diaryRepository = diaryRepository,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            // Toggle theme and save preference
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
