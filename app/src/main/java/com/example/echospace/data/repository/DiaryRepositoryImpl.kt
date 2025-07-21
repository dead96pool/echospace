// /app/src/main/java/com/example/echospace/data/repository/DiaryRepositoryImpl.kt

package com.example.echospace.data.repository

import androidx.documentfile.provider.DocumentFile
import com.example.echospace.data.local.FileManager
import com.example.echospace.data.local.FileOperationResult
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.SortOption
import com.example.echospace.domain.repository.DiaryRepository
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiaryRepositoryImpl @Inject constructor(
    private val fileManager: FileManager
) : DiaryRepository {

    override suspend fun getEntries(sortOption: SortOption): List<DiaryEntry> {
        return withContext(Dispatchers.IO) {
            try {
                val entries = fileManager.getFiles()
                    .filter { file ->
                        // Filter out corrupted files
                        isFileValid(file)
                    }
                    .map { file ->
                        DiaryEntry(
                            file = file,
                            name = file.name?.removeSuffix(".txt") ?: "Untitled"
                        )
                    }

                return@withContext when (sortOption) {
                    SortOption.NAME_ASC -> entries.sortedBy { it.name.lowercase() }
                    SortOption.NAME_DESC -> entries.sortedByDescending { it.name.lowercase() }
                    SortOption.DATE_NEWEST -> entries.sortedByDescending { it.file.lastModified() }
                    SortOption.DATE_OLDEST -> entries.sortedBy { it.file.lastModified() }
                }
            } catch (e: Exception) {
                Log.e("DiaryRepository", "Error getting entries", e)
                emptyList()
            }
        }
    }

    /**
     * Validates if a file is accessible and not corrupted
     */
    private suspend fun isFileValid(file: DocumentFile): Boolean {
        return try {
            // Basic validation checks
            if (!file.exists() || !file.canRead() || file.length() < 0) {
                return false
            }

            // Try to read a small portion to verify accessibility
            fileManager.readFile(file).take(10)
            true
        } catch (e: Exception) {
            Log.w("DiaryRepository", "File validation failed for ${file.name}", e)
            false
        }
    }

    override suspend fun createEntry(fileName: String): DocumentFile? {
        return fileManager.createFile(fileName)
    }

    override suspend fun readContent(file: DocumentFile): String {
        return fileManager.readFile(file)
    }

    override suspend fun deleteEntry(entry: DiaryEntry): Boolean {
        return fileManager.deleteFile(entry.file)
    }

    override suspend fun renameEntry(entry: DiaryEntry, newName: String): DiaryEntry? {
        val newFile = fileManager.renameFile(entry.file, newName)
        return newFile?.let { DiaryEntry(it, newName) }
    }

    override suspend fun saveEntry(file: DocumentFile, content: String): FileOperationResult {
        return fileManager.saveFileWithBackup(file, content)
    }
}
