package com.example.echospace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.echospace.data.local.FileManager
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.ui.home.HomeScreen
import com.example.echospace.ui.theme.EchoSpaceTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repository
        val fileManager = FileManager(this)
        val diaryRepository = DiaryRepositoryImpl(fileManager)

        setContent {
            EchoSpaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(diaryRepository)
                }
            }
        }
    }

    // Add this to refresh when returning from editor
    override fun onResume() {
        super.onResume()
        // This will trigger recomposition when returning to main activity
    }
}