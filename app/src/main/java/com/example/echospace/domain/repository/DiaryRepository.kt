// /app/src/main/java/com/example/echospace/domain/repository/DiaryRepository.kt

package com.example.echospace.domain.repository

import androidx.documentfile.provider.DocumentFile
import com.example.echospace.data.local.FileOperationResult
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.SortOption

interface DiaryRepository {
    suspend fun getEntries(sortOption: SortOption = SortOption.DATE_NEWEST): List<DiaryEntry>
    suspend fun createEntry(fileName: String): DocumentFile?
    suspend fun readContent(file: DocumentFile): String
    suspend fun deleteEntry(entry: DiaryEntry): Boolean
    suspend fun renameEntry(entry: DiaryEntry, newName: String): DiaryEntry?
    suspend fun saveEntry(file: DocumentFile, content: String): FileOperationResult
}
