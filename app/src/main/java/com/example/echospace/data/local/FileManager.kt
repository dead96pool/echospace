// /app/src/main/java/com/example/echospace/data/local/FileManager.kt

package com.example.echospace.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class FileManager(private val context: Context) {

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

    suspend fun createFile(fileName: String): DocumentFile? = withContext(Dispatchers.IO) {
        val folderUri = getFolderUri() ?: return@withContext null
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext null
        val existingFile = folder.findFile("$fileName.txt")
        if (existingFile != null) return@withContext null // File already exists

        folder.createFile("text/plain", "$fileName.txt")
    }

    suspend fun readFile(file: DocumentFile): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(file.uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        } catch (e: Exception) { "" }
    }

    suspend fun writeFile(file: DocumentFile, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(file.uri, "wt")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use {
                    it.write(content.toByteArray(Charsets.UTF_8))
                    it.flush() // Ensure content is written
                }
                pfd.close()
            }
            true
        } catch (e: Exception) { false }
    }
}
