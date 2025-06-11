// data/local/FileManager.kt

package com.example.echospace.data.local

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
//import java.io.File
//import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext

@Stable
data class FileMetadata(
    val documentFile: DocumentFile,
    val title: String,
    val lastModified: Long,
    val wordCount: Int,
    val tags: List<String>
)

// Add new cache structure for per-file tracking
data class FileCache(
    val uri: String,
    val lastModified: Long,
    val metadata: FileMetadata,
    val cacheTimestamp: Long = System.currentTimeMillis()
)

class FileManager(private val context: Context) {

    companion object {
        private const val PREF_FOLDER_URI = "echospace_folder_uri"
        private const val MAX_CACHE_SIZE = 500 // Limit cache size
        private const val CACHE_CLEANUP_THRESHOLD = 600
        private const val CACHE_EXPIRY_HOURS = 24
    }

    private val sharedPrefs = context.getSharedPreferences("echospace_prefs", Context.MODE_PRIVATE)


    // Replace List cache with Map for per-file caching
    private var fileCache: MutableMap<String, FileCache> = mutableMapOf()
    private var lastFolderScan: Long = 0
    private val folderScanInterval = 2000L // Check for new/deleted files every 2 seconds
    private var isCacheRefreshing = false


    // Get the user-selected folder URI
    private fun getFolderUri(): Uri? {
        val uriString = sharedPrefs.getString(PREF_FOLDER_URI, null)
        return uriString?.toUri()
    }

    // Save the selected folder URI
    fun saveFolderUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        sharedPrefs.edit {
            putString(PREF_FOLDER_URI, uri.toString())
        }
    }

    // Check if folder is selected
    fun isFolderSelected(): Boolean = getFolderUri() != null

    // Get all files with cached metadata for performance
    fun getAllFilesWithMetadata(): List<FileMetadata> {
        val currentTime = System.currentTimeMillis()

        // Clean expired cache entries first
        cleanExpiredCache()

        // Check if we need to scan for new/deleted files
        if (currentTime - lastFolderScan > folderScanInterval) {
            updateFileList()
            lastFolderScan = currentTime
        }

        // Clean cache if it's too large
        cleanupCacheIfNeeded()

        // Return cached metadata, sorted by last modified (same as before)
        return fileCache.values.map { it.metadata }.sortedByDescending { it.lastModified }
    }

    private fun updateFileList() {
        if (isCacheRefreshing) return
        isCacheRefreshing = true

        try {
            val folderUri = getFolderUri() ?: return
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return

            val currentFiles = folder.listFiles()
                .filter { it.isFile && it.name?.endsWith(".txt") == true }

            val currentFileUris = currentFiles.map { it.uri.toString() }.toSet()
            val cachedFileUris = fileCache.keys.toSet()

            // Remove deleted files from cache
            val deletedFiles = cachedFileUris - currentFileUris
            deletedFiles.forEach { fileCache.remove(it) }

            // Check each current file - only update if modified
            currentFiles.forEach { documentFile ->
                val fileUri = documentFile.uri.toString()
                val fileLastModified = documentFile.lastModified()
                val cachedFile = fileCache[fileUri]

                // Update cache ONLY if file is new or actually modified
                if (cachedFile == null || cachedFile.lastModified != fileLastModified) {
                    updateFileMetadata(documentFile, fileUri, fileLastModified)
                }
            }
        } finally {
            isCacheRefreshing = false
        }
    }

    private fun updateFileMetadata(documentFile: DocumentFile, fileUri: String, fileLastModified: Long) {
        try {
            val content = readFileContent(documentFile)
            val metadata = FileMetadata(
                documentFile = documentFile,
                title = documentFile.name?.removeSuffix(".txt") ?: "Untitled",
                lastModified = fileLastModified,
                wordCount = countWordsFromContent(content),
                tags = parseTagsFromContent(content)
            )

            fileCache[fileUri] = FileCache(
                uri = fileUri,
                lastModified = fileLastModified,
                metadata = metadata,
                cacheTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Handle error gracefully - don't break the app
        }
    }

    // Cache management functions
    private fun cleanExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val expiryTime = CACHE_EXPIRY_HOURS * 60 * 60 * 1000L

        val expiredEntries = fileCache.filter { (_, cache) ->
            currentTime - cache.cacheTimestamp > expiryTime
        }

        expiredEntries.keys.forEach { fileCache.remove(it) }
    }

    private fun cleanupCacheIfNeeded() {
        if (fileCache.size > CACHE_CLEANUP_THRESHOLD) {
            // Keep only the most recently cached files
            val sortedEntries = fileCache.entries.sortedByDescending { it.value.cacheTimestamp }
            fileCache.clear()
            sortedEntries.take(MAX_CACHE_SIZE).forEach { (key, value) ->
                fileCache[key] = value
            }
        }
    }

    // Update specific file when it's modified
    private fun updateFileCache(documentFile: DocumentFile) {
        val fileUri = documentFile.uri.toString()
        val fileLastModified = documentFile.lastModified()
        updateFileMetadata(documentFile, fileUri, fileLastModified)
    }

    // Remove file from cache
    private fun removeFileFromCache(documentFile: DocumentFile) {
        fileCache.remove(documentFile.uri.toString())
    }

    // Add function to get cached word count (for DiaryRepositoryImpl)
    fun getCachedWordCount(documentFile: DocumentFile): Int {
        return fileCache[documentFile.uri.toString()]?.metadata?.wordCount ?: 0
    }

    // Optimized word counting from content (avoids re-reading file)
    private fun countWordsFromContent(content: String): Int {
        return try {
            val allLines = content.lines()
            if (allLines.isEmpty()) return 0

            val contentLines = allLines.toMutableList()

            // Remove title line if present (same logic as FileUtils)
            if (contentLines.isNotEmpty()) {
                val firstLineTrimmed = contentLines.first().trim()
                val titlePattern = Regex("""^#\s+.+\s+\d{1,2}:\d{2}\s+(AM|PM)$""", RegexOption.IGNORE_CASE)
                if (titlePattern.matches(firstLineTrimmed)) {
                    contentLines.removeAt(0)
                }
            }

            // Remove hashtag lines at end (same logic as FileUtils)
            if (contentLines.isNotEmpty()) {
                var lastNonBlankLineIndex = -1
                for (i in contentLines.indices.reversed()) {
                    if (contentLines[i].isNotBlank()) {
                        lastNonBlankLineIndex = i
                        break
                    }
                }

                if (lastNonBlankLineIndex != -1) {
                    val potentialHashtagLine = contentLines[lastNonBlankLineIndex].trim()
                    if (potentialHashtagLine.startsWith("#")) {
                        val subListToRemove = contentLines.subList(lastNonBlankLineIndex, contentLines.size)
                        subListToRemove.clear()
                    }
                }
            }

            if (contentLines.all { it.isBlank() }) return 0

            val actualContent = contentLines.joinToString(separator = " ")
            return actualContent.split(Regex("\\s+")).count { it.isNotBlank() }
        } catch (e: Exception) {
            0
        }
    }

    // Parse tags from content (avoids re-reading file)
    private fun parseTagsFromContent(content: String): List<String> {
        return try {
            val lines = content.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return emptyList()

            val lastLine = lines.last().trim()
            if (lastLine.startsWith("#")) {
                lastLine.split(" ")
                    .filter { it.startsWith("#") && it.length > 1 }
                    .map { it.substring(1) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Create file with given name
    fun createFile(fileName: String, currentPath: String): DocumentFile? {
        val folderUri = getFolderUri() ?: return null
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null

        val sanitizedName = sanitizeFileName(fileName)
        val documentFile = folder.createFile("text/plain", "$sanitizedName.txt") ?: return null

        // Keep your existing file creation logic
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
            .uppercase()
            .replace(".", "")
        val firstLine = "# $sanitizedName  $currentTime\n\n"

        val content = if (currentPath.isNotEmpty()) {
            val hashtagPath = "#${currentPath.replace("/", "#")}"
            "$firstLine\n\n$hashtagPath"
        } else {
            firstLine
        }

        val success = writeToFile(documentFile, content)
        if (success) {
            updateFileCache(documentFile) // Update only this file's cache
        }
        return if (success) documentFile else null
    }

    fun createFileWithCurrentDate(currentPath: String): DocumentFile? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fileName = dateFormat.format(Date())

        val folderUri = getFolderUri() ?: return null
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null

        val documentFile = folder.createFile("text/plain", "$fileName.txt") ?: return null

        // Add date and time as first line of editable content
        val displayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentDate = Date()
        val displayDate = displayDateFormat.format(currentDate)
        val displayTime = timeFormat.format(currentDate)
            .uppercase()
            .replace(".", "")
        val firstLine = "# $displayDate  $displayTime\n\n"

        // Add hashtag categorization based on current path
        val content = if (currentPath.isNotEmpty()) {
            val hashtagPath = "#${currentPath.replace("/", "#")}"
            "$firstLine\n\n$hashtagPath"
        } else {
            firstLine
        }

        val success = writeToFile(documentFile, content)
        if (success) {
            updateFileCache(documentFile) // Invalidate cache after creating
        }
        return if (success) documentFile else null
    }

    // Write content to file
    private fun writeToFile(documentFile: DocumentFile, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(documentFile.uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Read file content
    fun readFileContent(documentFile: DocumentFile): String {
        return try {
            context.contentResolver.openInputStream(documentFile.uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // Write file content
    fun writeFileContent(documentFile: DocumentFile, content: String): Boolean {
        val success = writeToFile(documentFile, content)
        if (success) {
            updateFileCache(documentFile) // Update only this file's cache
        }
        return success
    }

    // Delete file
    fun deleteFile(documentFile: DocumentFile): Boolean {
        val success = documentFile.delete()
        if (success) {
            removeFileFromCache(documentFile) // Remove only this file from cache
        }
        return success
    }


    // Rename file
    fun renameFile(documentFile: DocumentFile, newName: String): Boolean {
        val sanitizedName = sanitizeFileName(newName)

        // Check if a file with the new name already exists
        if (fileExists(sanitizedName)) {
            return false // Return false to indicate failure due to existing file
        }

        val oldUri = documentFile.uri.toString()
        val success = documentFile.renameTo("$sanitizedName.txt")
        if (success) {
            // Remove old cache entry and update with new file reference
            fileCache.remove(oldUri)
            updateFileCache(documentFile)
        }
        return success
    }

    // Check if file exists
    fun fileExists(fileName: String): Boolean {
        val folderUri = getFolderUri() ?: return false
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        val sanitizedName = sanitizeFileName(fileName)

        return folder.listFiles().any {
            it.name == "$sanitizedName.txt"
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        // Remove invalid file name characters
        return fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}
