// data/local/FileManager.kt

package com.example.echospace.data.local

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
//import java.io.File
//import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileMetadata(
    val documentFile: DocumentFile,
    val title: String,
    val lastModified: Long,
    val wordCount: Int,
    val tags: List<String>
)

class FileManager(private val context: Context) {

    companion object {
        private const val PREF_FOLDER_URI = "echospace_folder_uri"
    }

    private val sharedPrefs = context.getSharedPreferences("echospace_prefs", Context.MODE_PRIVATE)

    // Cache for file metadata
    private var cachedMetadata: List<FileMetadata>? = null
    private var lastCacheTime: Long = 0
    private val cacheValidityMs = 5000L // 5 seconds cache validity

    // Add flag to prevent unnecessary cache refreshes
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

        // Return cached data if still valid or already refreshing
        if ((cachedMetadata != null && (currentTime - lastCacheTime) < cacheValidityMs) || isCacheRefreshing) {
            return cachedMetadata ?: emptyList()
        }

        // Prevent concurrent cache refreshes
        isCacheRefreshing = true

        try {
            // Your existing cache refresh logic...
            val folderUri = getFolderUri() ?: return emptyList()
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()

            cachedMetadata = folder.listFiles()
                .filter { it.isFile && it.name?.endsWith(".txt") == true }
                .map { documentFile ->
                    val content = readFileContent(documentFile)
                    FileMetadata(
                        documentFile = documentFile,
                        title = documentFile.name?.removeSuffix(".txt") ?: "Untitled",
                        lastModified = documentFile.lastModified(),
                        wordCount = countWordsFromContent(content),
                        tags = parseTagsFromContent(content)
                    )
                }
                .sortedByDescending { it.lastModified }

            lastCacheTime = currentTime
            return cachedMetadata!!
        } finally {
            isCacheRefreshing = false
        }
    }


    // Legacy function for compatibility - now uses cached metadata
    fun getAllFiles(): List<DocumentFile> {
        return getAllFilesWithMetadata().map { it.documentFile }
    }

    // Invalidate cache when files change
    fun invalidateCache() {
        cachedMetadata = null
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


        // Add filename and time as first line of editable content
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
            .uppercase()
            .replace(".", "")
        val firstLine = "# $sanitizedName  $currentTime\n\n"

        // Add hashtag categorization based on current path
        val content = if (currentPath.isNotEmpty()) {
            val hashtagPath = "#${currentPath.replace("/", "#")}"
            "$firstLine\n\n$hashtagPath"
        } else {
            firstLine
        }

        val success = writeToFile(documentFile, content)
        if (success) {
            invalidateCache() // Invalidate cache after creating
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
            invalidateCache() // Invalidate cache after creating
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
            invalidateCache() // Invalidate cache after writing
        }
        return success
    }

    // Delete file
    fun deleteFile(documentFile: DocumentFile): Boolean {
        val success = documentFile.delete()
        if (success) {
            invalidateCache() // Invalidate cache after deleting
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

        val success = documentFile.renameTo("$sanitizedName.txt")
        if (success) {
            invalidateCache() // Invalidate cache after renaming
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
