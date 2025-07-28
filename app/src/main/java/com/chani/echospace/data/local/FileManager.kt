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

class FileAlreadyExistsException(message: String) : Exception(message)

class FileManager @Inject constructor(
    private val context: Context
) {

    private val sharedPrefs = context.getSharedPreferences("echospace_prefs", Context.MODE_PRIVATE)

    private fun getFolderUri(): Uri? {
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

    suspend fun getFiles(): List<DocumentFile> = withContext(Dispatchers.IO) {
        val folderUri = getFolderUri() ?: return@withContext emptyList()
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        folder.listFiles().filter { it.isFile && it.name?.endsWith(".txt") == true }
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

}
