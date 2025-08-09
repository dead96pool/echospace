// /app/src/main/java/com/chani/echospace/data/local/FileManager.kt

package com.chani.echospace.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import javax.inject.Inject
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter

class FileAlreadyExistsException(message: String) : Exception(message)
class FolderAccessLostException(message: String) : Exception(message)

class FileManager @Inject constructor(
    private val context: Context
) {

    private val sharedPrefs = context.getSharedPreferences("echospace_prefs", Context.MODE_PRIVATE)

    fun getFolderUri(): Uri? {
        val uriString = sharedPrefs.getString("folder_uri", null)
        return uriString?.toUri()
    }

    fun saveFolderUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        sharedPrefs.edit { putString("folder_uri", uri.toString()) }
    }

    fun isFolderSelected(): Boolean = getFolderUri() != null

    // to detect folder access loss
    private fun validateFolderAccess(): Boolean {
        val folderUri = getFolderUri() ?: return false
        return try {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
            folder?.exists() == true && folder.canRead()
        } catch (e: SecurityException) {
            Log.w("FileManager", "Folder access revoked", e)
            false
        } catch (e: Exception) {
            Log.w("FileManager", "Folder validation failed", e)
            false
        }
    }

    // updated function to handle folder access loss
    suspend fun getFiles(): List<DocumentFile> {
        return withContext(Dispatchers.IO) {
            // First validate folder access
            if (!validateFolderAccess()) {
                throw FolderAccessLostException("Folder access has been revoked or lost")
            }

            val folderUri = getFolderUri() ?: return@withContext emptyList()
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()

            folder.listFiles().filter { it.isFile && it.name?.endsWith(".txt") == true }
        }
    }

    suspend fun createFile(fileName: String): DocumentFile? {
        return withContext(Dispatchers.IO) {
            val folderUri = getFolderUri() ?: return@withContext null
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext null

            // Use case-insensitive check
            val existingFile = findFileIgnoreCase(folder, fileName)
            if (existingFile != null) {
                // Throw the custom exception
                throw FileAlreadyExistsException("File ${fileName}.txt already exists")
            }

            folder.createFile("text/plain", "$fileName.txt")
        }
    }

    suspend fun readFile(file: DocumentFile): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(file.uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        } catch (e: Exception) { "" }
    }

    suspend fun deleteFile(file: DocumentFile): Boolean = withContext(Dispatchers.IO) {
        return@withContext file.delete()
    }

    suspend fun renameFile(file: DocumentFile, newName: String): DocumentFile? {
        return withContext(Dispatchers.IO) {
            val folderUri = getFolderUri() ?: return@withContext null
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext null

            // Use case-insensitive check
            val existingFile = findFileIgnoreCase(folder, newName)
            if (existingFile != null && existingFile.uri != file.uri) {
                // Throw the same exception used for create operations
                throw FileAlreadyExistsException("File ${newName}.txt already exists")
            }

            if (file.renameTo("$newName.txt")) {
                // After renaming, get the new DocumentFile object
                folder.findFile("$newName.txt")
            } else {
                null
            }
        }
    }

    /**
     * Creates a temporary backup of a file before editing
     */
    private suspend fun createBackup(originalFile: DocumentFile): DocumentFile? {
        return withContext(Dispatchers.IO) {
            try {
                val backupName = "${originalFile.name}.backup"
                val parentDir = originalFile.parentFile ?: return@withContext null

                // Create backup file
                val backupFile = parentDir.createFile("text/plain", backupName)

                if (backupFile != null) {
                    // Copy original content to backup
                    context.contentResolver.openInputStream(originalFile.uri)?.use { input ->
                        context.contentResolver.openOutputStream(backupFile.uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    backupFile
                } else null
            } catch (e: Exception) {
                Log.e("FileManager", "Failed to create backup", e)
                null
            }
        }
    }

    /**
     * Restores content from backup file
     */
    private suspend fun restoreFromBackup(originalFile: DocumentFile, backupFile: DocumentFile): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(backupFile.uri)?.use { input ->
                    context.contentResolver.openOutputStream(originalFile.uri, "wt")?.use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                Log.e("FileManager", "Failed to restore from backup", e)
                false
            }
        }
    }

    /**
     * Safely saves content to a file with backup protection
     */
    suspend fun saveFileWithBackup(file: DocumentFile, content: String): FileOperationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Create backup of original file
                val backupFile = createBackup(file)

                try {
                    // Step 2: Write new content to original file
                    context.contentResolver.openOutputStream(file.uri, "wt")?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                        outputStream.flush()
                    }

                    // Step 3: If successful, delete backup
                    backupFile?.delete()

                    FileOperationResult.Success
                } catch (writeException: Exception) {
                    // Step 4: If write failed, restore from backup
                    backupFile?.let { backup ->
                        val restored = restoreFromBackup(file, backup)
                        backup.delete()

                        if (!restored) {
                            Log.e("FileManager", "Failed to restore backup after write failure")
                        }
                    }

                    FileOperationResult.Error("Failed to save file: ${writeException.message}")
                }
            } catch (e: Exception) {
                FileOperationResult.Error("File operation failed: ${e.message}")
            }
        }
    }

    /**
     * Finds a file with case-insensitive name matching
     */
    private fun findFileIgnoreCase(folder: DocumentFile, fileName: String): DocumentFile? {
        val targetName = "$fileName.txt".lowercase()
        return folder.listFiles().find { file ->
            file.name?.lowercase() == targetName
        }
    }

    /**
     * Creates or gets the exports subfolder within the user's selected folder
     */
    private suspend fun getOrCreateExportsFolder(): DocumentFile? {
        return withContext(Dispatchers.IO) {
            val folderUri = getFolderUri() ?: return@withContext null
            val parentFolder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext null

            // Check if exports folder already exists
            val existingExports = parentFolder.findFile("exports")
            if (existingExports?.isDirectory == true) {
                return@withContext existingExports
            }

            // Create exports folder if it doesn't exist
            parentFolder.createDirectory("exports")
        }
    }

    /**
     * Gets all .txt files from the user folder (excluding exports subfolder)
     */
    private suspend fun getTxtFilesForExport(): List<DocumentFile> {
        return withContext(Dispatchers.IO) {
            try {
                val allFiles = getFiles() // Uses existing getFiles method
                // Filter out any files from the exports subfolder to avoid nested zips
                allFiles.filter { file ->
                    val parentName = file.parentFile?.name
                    parentName != "exports" // Exclude exports folder
                }
            } catch (e: Exception) {
                Log.e("FileManager", "Failed to get txt files for export", e)
                emptyList()
            }
        }
    }

    /**
     * Exports all .txt files to a zip with UTC timestamp
     */
    suspend fun exportFilesToZip(): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                // Get files to export
                val txtFiles = getTxtFilesForExport()
                if (txtFiles.isEmpty()) {
                    return@withContext ExportResult.NoFiles
                }

                // Create exports folder
                val exportsFolder = getOrCreateExportsFolder()
                    ?: return@withContext ExportResult.Error("Failed to create exports folder")

                // Generate UTC timestamp filename
                val timestamp = Instant.now().atZone(java.time.ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
                val zipFileName = "echospace_$timestamp.zip"

                // Create zip file in exports folder
                val zipFile = exportsFolder.createFile("application/zip", zipFileName)
                    ?: return@withContext ExportResult.Error("Failed to create zip file")

                // Write zip content
                context.contentResolver.openOutputStream(zipFile.uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zipOut ->
                        txtFiles.forEach { file ->
                            try {
                                val content = readFile(file) // Uses existing readFile method
                                val fileName = file.name ?: "untitled.txt"

                                val zipEntry = ZipEntry(fileName)
                                zipOut.putNextEntry(zipEntry)
                                zipOut.write(content.toByteArray(Charsets.UTF_8))
                                zipOut.closeEntry()
                            } catch (e: Exception) {
                                Log.w("FileManager", "Failed to add file ${file.name} to zip", e)
                            }
                        }
                    }
                }

                // Get the full path for display
                val folderPath = extractDisplayPath(getFolderUri().toString())
                val fullPath = "$folderPath/exports/$zipFileName"

                ExportResult.Success(
                    fileName = zipFileName,
                    fullPath = fullPath,
                    fileCount = txtFiles.size
                )

            } catch (e: Exception) {
                Log.e("FileManager", "Export failed", e)
                ExportResult.Error("Export failed: ${e.message}")
            }
        }
    }

    /**
     * Save the timestamp of the last successful export
     */
    fun saveLastExportTimestamp(timestamp: Instant) {
        sharedPrefs.edit {
            putString("last_export_timestamp", timestamp.toString())
        }
    }

    /**
     * Get the timestamp of the last successful export
     */
    fun getLastExportTimestamp(): Instant? {
        val timestampString = sharedPrefs.getString("last_export_timestamp", null)
        return timestampString?.let {
            try {
                Instant.parse(it)
            } catch (e: Exception) {
                Log.w("FileManager", "Failed to parse last export timestamp", e)
                null
            }
        }
    }

    private fun extractDisplayPath(uriString: String): String {
        return try {
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

// Export result sealed class
sealed class ExportResult {
    data class Success(
        val fileName: String,
        val fullPath: String,
        val fileCount: Int
    ) : ExportResult()

    data class Error(val message: String) : ExportResult()
    data object NoFiles : ExportResult()
}
