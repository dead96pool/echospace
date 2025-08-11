// /app/src/main/java/com/example/echospace/domain/repository/DiaryRepository.kt

package com.chani.echospace.domain.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.chani.echospace.data.local.FileOperationResult
import com.chani.echospace.domain.model.DiaryEntry
import com.chani.echospace.domain.model.SortOption

data class VirtualFolder(
    val nameDisplay: String,
    val nameKey: String,
    val fullPathKeys: List<String>,
    val mostRecentMillis: Long
)

data class VirtualListing(
    val folders: List<VirtualFolder>,
    val files: List<DiaryEntry>
)

interface DiaryRepository {
    suspend fun getEntries(sortOption: SortOption = SortOption.DATE_NEWEST): List<DiaryEntry>
    suspend fun createEntry(fileName: String): DocumentFile?
    suspend fun readContent(file: DocumentFile): String
    suspend fun deleteEntry(entry: DiaryEntry): Boolean
    suspend fun renameEntry(entry: DiaryEntry, newName: String): DiaryEntry?
    suspend fun saveEntry(file: DocumentFile, content: String): FileOperationResult
    suspend fun refreshAllMetadata()
    suspend fun saveFolderUri(uri: Uri)
    suspend fun getVirtualListing(pathKeys: List<String>, sort: SortOption): VirtualListing
}
