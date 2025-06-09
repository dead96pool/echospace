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

class FileManager(private val context: Context) {

    companion object {
        private const val PREF_FOLDER_URI = "echospace_folder_uri"
    }

    private val sharedPrefs = context.getSharedPreferences("echospace_prefs", Context.MODE_PRIVATE)

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

    // Get all text files from the selected folder
    fun getAllFiles(): List<DocumentFile> {
        val folderUri = getFolderUri() ?: return emptyList()
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()

        return folder.listFiles()
            .filter { it.isFile && it.name?.endsWith(".txt") == true }
            .sortedByDescending { it.lastModified() }
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

        return if (writeToFile(documentFile, content)) documentFile else null
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

        return if (writeToFile(documentFile, content)) documentFile else null
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
        return writeToFile(documentFile, content)
    }

    // Delete file
    fun deleteFile(documentFile: DocumentFile): Boolean {
        return documentFile.delete()
    }

    // Add this function to your FileManager.kt
    fun fileExistsWithName(fileName: String): Boolean {
        val folderUri = getFolderUri() ?: return false
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        val sanitizedName = sanitizeFileName(fileName)

        return folder.listFiles().any {
            it.name == "$sanitizedName.txt"
        }
    }

    // Rename file
    fun renameFile(documentFile: DocumentFile, newName: String): Boolean {
        val sanitizedName = sanitizeFileName(newName)

        // Check if a file with the new name already exists
        if (fileExistsWithName(sanitizedName)) {
            return false // Return false to indicate failure due to existing file
        }

        return documentFile.renameTo("$sanitizedName.txt")
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
