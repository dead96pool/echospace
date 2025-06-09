// domain/repository/DiaryRepository.kt
package com.example.echospace.domain.repository

import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.FolderItem
import com.example.echospace.domain.model.CreateFileResult
import androidx.documentfile.provider.DocumentFile
//import java.io.File

enum class SortType {
    NAME_ASC,
    NAME_DESC,
    TIME_ASC,
    TIME_DESC
}

interface DiaryRepository {
    fun getAllEntries(): List<DiaryEntry>
    fun createEntry(fileName: String, currentPath: String): DocumentFile?
    fun createEntryWithCurrentDate(currentPath: String): DocumentFile?
    fun readEntryContent(file: DocumentFile): String
    fun writeEntryContent(file: DocumentFile, content: String): Boolean
    fun renameEntry(file: DocumentFile, newName: String): Boolean
    fun deleteEntry(file: DocumentFile): Boolean
    fun getFolderContents(currentPath: String): List<FolderItem>
    fun getParentPath(currentPath: String): String?
    fun hasContentInPath(path: String): Boolean
    fun createEntryWithResult(fileName: String, currentPath: String): Pair<CreateFileResult, DocumentFile?>
    fun createEntryWithCurrentDateResult(currentPath: String): Pair<CreateFileResult, DocumentFile?>
    fun getFolderContentsSorted(currentPath: String, sortType: SortType): List<FolderItem>
    fun searchEntries(query: String, currentPath: String): List<FolderItem>
}
