// data/repository/DiaryRepositoryImpl.kt
package com.example.echospace.data.repository

import com.example.echospace.data.local.FileManager
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.FolderItem
import com.example.echospace.domain.repository.DiaryRepository
import com.example.echospace.utils.VirtualFolderManager
import java.io.File

class DiaryRepositoryImpl(private val fileManager: FileManager) : DiaryRepository {

    private val virtualFolderManager = VirtualFolderManager()

    override fun getAllEntries(): List<DiaryEntry> {
        return fileManager.getAllFiles().map { DiaryEntry.fromFile(it) }
    }

    override fun createEntry(fileName: String): File? {
        return fileManager.createFile(fileName)
    }

    override fun createEntryWithCurrentDate(): File? {
        return fileManager.createFileWithCurrentDate()
    }

    override fun readEntryContent(file: File): String {
        return fileManager.readFileContent(file)
    }

    override fun writeEntryContent(file: File, content: String): Boolean {
        return fileManager.writeFileContent(file, content)
    }

    override fun renameEntry(file: File, newName: String): Boolean {
        return fileManager.renameFile(file, newName)
    }

    override fun deleteEntry(file: File): Boolean {
        return fileManager.deleteFile(file)
    }

    override fun getFolderContents(currentPath: String): List<FolderItem> {
        val allEntries = getAllEntries()
        return virtualFolderManager.getFolderContents(allEntries, currentPath)
    }

    override fun getParentPath(currentPath: String): String? {
        return virtualFolderManager.getParentPath(currentPath)
    }

    override fun hasContentInPath(path: String): Boolean {
        val allEntries = getAllEntries()
        return virtualFolderManager.hasContentInPath(path, allEntries)
    }

    fun findNearestParentWithContent(currentPath: String): String {
        val allEntries = getAllEntries()
        return virtualFolderManager.findNearestParentWithContent(currentPath, allEntries)
    }
}
