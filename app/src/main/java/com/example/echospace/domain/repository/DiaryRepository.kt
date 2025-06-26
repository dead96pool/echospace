// /app/src/main/java/com/example/echospace/domain/repository/DiaryRepository.kt

package com.example.echospace.domain.repository

import androidx.documentfile.provider.DocumentFile
import com.example.echospace.domain.model.DiaryEntry

interface DiaryRepository {
    suspend fun getEntries(): List<DiaryEntry>
    suspend fun createEntry(fileName: String): DocumentFile?
    suspend fun readContent(file: DocumentFile): String
    suspend fun writeContent(file: DocumentFile, content: String)
}
