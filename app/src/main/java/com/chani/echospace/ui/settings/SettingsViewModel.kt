package com.chani.echospace.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.echospace.data.local.FileManager
import com.chani.echospace.data.local.ExportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val fileManager: FileManager
) : ViewModel() {

    private val _folderPath = MutableStateFlow<String?>(null)
    val folderPath: StateFlow<String?> = _folderPath.asStateFlow()

    private val _exportPath = MutableStateFlow<String?>(null)
    val exportPath: StateFlow<String?> = _exportPath.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    private val _showExportError = MutableStateFlow(false)
    val showExportError: StateFlow<Boolean> = _showExportError.asStateFlow()

    private val _exportSuccess = MutableStateFlow<ExportResult.Success?>(null)
    val exportSuccess: StateFlow<ExportResult.Success?> = _exportSuccess.asStateFlow()

    private val _showSuccessIcon = MutableStateFlow(false)
    val showSuccessIcon: StateFlow<Boolean> = _showSuccessIcon.asStateFlow()

    private val _lastExportTimestamp = MutableStateFlow<String?>(null)
    val lastExportTimestamp: StateFlow<String?> = _lastExportTimestamp.asStateFlow()


    init {
        loadFolderPath()
        loadLastExportTimestamp() // Load saved timestamp on init
    }

    private fun loadLastExportTimestamp() {
        viewModelScope.launch {
            val timestamp = fileManager.getLastExportTimestamp()
            _lastExportTimestamp.value = timestamp?.let { formatTimestamp(it) }
        }
    }

    private fun loadFolderPath() {
        viewModelScope.launch {
            // Get the folder URI and extract display path
            val uri = fileManager.getFolderUri()
            _folderPath.value = uri?.let { extractDisplayPath(it.toString()) }

            // Set export path
            _exportPath.value = _folderPath.value?.let { "$it/exports" }
        }
    }

    fun exportFiles() {
        viewModelScope.launch {
            _isExporting.value = true
            _exportError.value = null
            _showExportError.value = false
            _showSuccessIcon.value = false
            _exportSuccess.value = null

            when (val result = fileManager.exportFilesToZip()) {
                is ExportResult.Success -> {
                    _isExporting.value = false
                    _exportSuccess.value = result

                    // Save current timestamp and update UI
                    val currentTimestamp = Instant.now()
                    fileManager.saveLastExportTimestamp(currentTimestamp)
                    _lastExportTimestamp.value = formatTimestamp(currentTimestamp)

                    // Show DoneAll icon for exactly 2 seconds
                    _showSuccessIcon.value = true
                    kotlinx.coroutines.delay(2000) // 2 seconds
                    _showSuccessIcon.value = false // Revert to archive icon
                }
                is ExportResult.Error -> {
                    _isExporting.value = false
                    _exportError.value = result.message
                    _showExportError.value = true

                    // Hide error after 2 seconds
                    kotlinx.coroutines.delay(3000)
                    _showExportError.value = false
                }
                is ExportResult.NoFiles -> {
                    _isExporting.value = false
                    _exportError.value = "No files to export"
                    // This will be shown as snackbar, not error state
                }
            }
        }
    }

    // Format timestamp with complete timezone information
    private fun formatTimestamp(instant: Instant): String {
        return try {
            val systemZone = ZoneId.systemDefault()
            val zonedDateTime = instant.atZone(systemZone)

            val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' hh:mm:ss a")
            val timeZoneFormatter = DateTimeFormatter.ofPattern("(zzzz)")

            "${zonedDateTime.format(dateTimeFormatter)} ${zonedDateTime.format(timeZoneFormatter)}"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun clearExportSuccess() {
        _exportSuccess.value = null
    }

    fun clearExportError() {
        _exportError.value = null
        _showExportError.value = false
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
