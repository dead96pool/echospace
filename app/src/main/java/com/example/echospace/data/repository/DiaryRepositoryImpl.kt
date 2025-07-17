// /app/src/main/java/com/example/echospace/data/repository/DiaryRepositoryImpl.kt

package com.example.echospace.data.repository

import androidx.documentfile.provider.DocumentFile
import com.example.echospace.data.local.FileManager
import com.example.echospace.data.local.FileOperationResult
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.SortOption
import com.example.echospace.domain.repository.DiaryRepository
import javax.inject.Inject

class DiaryRepositoryImpl @Inject constructor(
    private val fileManager: FileManager
) : DiaryRepository {

    override suspend fun getEntries(sortOption: SortOption): List<DiaryEntry> {
        val entries = fileManager.getFiles()
            .map { DiaryEntry(file = it, name = it.name?.removeSuffix(".txt") ?: "Untitled") }

        return when (sortOption) {
            SortOption.NAME_ASC -> entries.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> entries.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST -> entries.sortedByDescending { it.file.lastModified() }
            SortOption.DATE_OLDEST -> entries.sortedBy { it.file.lastModified() }
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
