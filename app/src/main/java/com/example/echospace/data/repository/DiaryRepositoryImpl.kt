// /app/src/main/java/com/example/echospace/data/repository/DiaryRepositoryImpl.kt

package com.example.echospace.data.repository

import androidx.documentfile.provider.DocumentFile
import com.example.echospace.data.local.FileManager
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.repository.DiaryRepository

class DiaryRepositoryImpl(
    private val fileManager: FileManager
) : DiaryRepository {

    override suspend fun getEntries(): List<DiaryEntry> {
        return fileManager.getFiles()
            .map { DiaryEntry(file = it, name = it.name?.removeSuffix(".txt") ?: "Untitled") }
            .sortedByDescending { it.file.lastModified() }
    }

    override suspend fun createEntry(fileName: String): DocumentFile? {
        return fileManager.createFile(fileName)
    }

    override suspend fun readContent(file: DocumentFile): String {
        return fileManager.readFile(file)
    }

    override suspend fun writeContent(file: DocumentFile, content: String) {
        fileManager.writeFile(file, content)
    }
}
