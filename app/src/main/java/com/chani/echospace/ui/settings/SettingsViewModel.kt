package com.chani.echospace.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.echospace.data.local.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val fileManager: FileManager
) : ViewModel() {

    private val _folderPath = MutableStateFlow<String?>(null)
    val folderPath: StateFlow<String?> = _folderPath.asStateFlow()

    init {
        loadFolderPath()
    }

    private fun loadFolderPath() {
        viewModelScope.launch {
            // Get the folder URI and extract display path
            val uri = fileManager.getFolderUri()
            _folderPath.value = uri?.let { extractDisplayPath(it.toString()) }
        }
    }

    private fun extractDisplayPath(uriString: String): String {
        return try {
            // path as Documents/EchoSpace
            val decoded = java.net.URLDecoder.decode(uriString, "UTF-8")
            when {
                decoded.contains("primary:") -> {
                    val path = decoded.substringAfter("primary:")
                    path.ifEmpty { "Internal Storage" }
                }
                decoded.contains("Documents") -> {
                    decoded.substringAfter("tree/").substringAfter(":")
                }
                else -> "Selected Folder"
            }
        } catch (e: Exception) {
            "Selected Folder"
        }
    }
}
