// domain/repository/DiaryRepository.kt
package com.example.echospace.domain.repository

import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.FolderItem
import java.io.File

interface DiaryRepository {
    fun getAllEntries(): List<DiaryEntry>
    fun createEntry(fileName: String): File?
    fun createEntryWithCurrentDate(): File?
    fun readEntryContent(file: File): String
    fun writeEntryContent(file: File, content: String): Boolean
    fun renameEntry(file: File, newName: String): Boolean
    fun deleteEntry(file: File): Boolean

    fun getFolderContents(currentPath: String): List<FolderItem>
    fun getParentPath(currentPath: String): String?
    fun hasContentInPath(path: String): Boolean
}
