// /app/src/main/java/com/example/echospace/data/repository/DiaryRepositoryImpl.kt

package com.chani.echospace.data.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.chani.echospace.data.local.DiaryMetadata
import com.chani.echospace.data.local.DiaryMetadataDao
import com.chani.echospace.data.local.FileManager
import com.chani.echospace.data.local.FileOperationResult
import com.chani.echospace.data.local.MetadataUtils
import com.chani.echospace.domain.model.DiaryEntry
import com.chani.echospace.domain.model.SortOption
import com.chani.echospace.domain.repository.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.chani.echospace.data.local.FolderAccessLostException
import javax.inject.Inject


class DiaryRepositoryImpl @Inject constructor(
    private val fileManager: FileManager,
    private val metadataDao: DiaryMetadataDao
) : DiaryRepository {

    override suspend fun saveFolderUri(uri: Uri) {
        fileManager.saveFolderUri(uri)
    }

    override suspend fun getEntries(sortOption: SortOption): List<DiaryEntry> {
        return withContext(Dispatchers.IO) {
            try {
                // Get valid files (same logic as before)
                val files = fileManager.getFiles().filter { isFileValid(it) }

                // Create enhanced entries with metadata
                val entries = files.map { file ->
                    val metadata = getOrCalculateMetadata(file)
                    DiaryEntry(
                        file = file,
                        name = file.name?.removeSuffix(".txt") ?: "Untitled",
                        // include metadata if available
                        wordCount = metadata?.wordCount,
                        lastModifiedDisplay = metadata?.let {
                            MetadataUtils.formatLastModified(it.lastModified)
                        }
                    )
                }

                return@withContext when (sortOption) {
                    SortOption.NAME_ASC -> entries.sortedBy { it.name.lowercase() }
                    SortOption.NAME_DESC -> entries.sortedByDescending { it.name.lowercase() }
                    SortOption.DATE_NEWEST -> entries.sortedByDescending { it.file.lastModified() }
                    SortOption.DATE_OLDEST -> entries.sortedBy { it.file.lastModified() }
                }
            } catch (e: FolderAccessLostException) {
                // Re-throw this specific exception to be handled by the ViewModel
                throw e
            } catch (e: Exception) {
                Log.e("DiaryRepository", "Error getting entries", e)
                emptyList()
            }
        }
    }

    /**
     * NEW: Smart metadata management
     * Gets cached metadata or calculates if needed
     */
    private suspend fun getOrCalculateMetadata(file: DocumentFile): DiaryMetadata? {
        return try {
            val fileUri = file.uri.toString()
            val currentLastModified = file.lastModified()

            // Check if we have up-to-date cached metadata
            val cachedMetadata = metadataDao.getMetadata(fileUri)

            if (cachedMetadata != null && cachedMetadata.lastModified == currentLastModified) {
                // Cached metadata is current - return it (fast path)
                return cachedMetadata
            }

            // Need to calculate new metadata (slow path, but cached afterward)
            val content = fileManager.readFile(file)
            val wordCount = MetadataUtils.countWords(content)

            val newMetadata = DiaryMetadata(
                fileUri = fileUri,
                wordCount = wordCount,
                lastModified = currentLastModified,
                fileName = file.name ?: "Untitled"
            )

            // Cache the new metadata for future use
            metadataDao.insertMetadata(newMetadata)
            newMetadata

        } catch (e: Exception) {
            Log.w("DiaryRepository", "Failed to get metadata for ${file.name}", e)
            null // Graceful fallback - entry will display without metadata
        }
    }

    /**
     * File validation (enhanced with better error handling)
     */
    private fun isFileValid(file: DocumentFile): Boolean {
        return try {
            file.exists() && file.canRead() && file.length() >= 0
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

    /**
     * Enhanced deleteEntry with metadata cleanup
     * Maintains existing functionality while cleaning cache
     */
    override suspend fun deleteEntry(entry: DiaryEntry): Boolean {
        // Same file deletion logic as before
        val result = fileManager.deleteFile(entry.file)

        // NEW: Clean up metadata cache after successful deletion
        if (result) {
            try {
                metadataDao.deleteMetadata(entry.file.uri.toString())
            } catch (e: Exception) {
                Log.w("DiaryRepository", "Failed to delete metadata", e)
                // Continue normally - file deletion was successful
            }
        }

        return result
    }

    /**
     * Enhanced renameEntry with metadata updates
     * Maintains existing functionality while updating cache
     */
    override suspend fun renameEntry(entry: DiaryEntry, newName: String): DiaryEntry? {
        // Same file renaming logic as before
        val newFile = fileManager.renameFile(entry.file, newName)

        if (newFile != null) {
            // NEW: Update metadata with new file information
            try {
                val oldUri = entry.file.uri.toString()
                val newUri = newFile.uri.toString()

                val existingMetadata = metadataDao.getMetadata(oldUri)
                if (existingMetadata != null) {
                    // Delete old metadata entry
                    metadataDao.deleteMetadata(oldUri)
                    // Insert with new URI and filename
                    metadataDao.insertMetadata(
                        existingMetadata.copy(
                            fileUri = newUri,
                            fileName = newName
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("DiaryRepository", "Failed to update metadata after rename", e)
                // Continue normally - rename was successful
            }

            return DiaryEntry(newFile, newName)
        }

        return null
    }

    /**
     * Enhanced saveEntry with automatic metadata updates
     * Maintains existing functionality while updating cache
     */
    override suspend fun saveEntry(file: DocumentFile, content: String): FileOperationResult {
        // Same file saving logic as before
        val result = fileManager.saveFileWithBackup(file, content)

        // NEW: Update metadata cache after successful save
        if (result is FileOperationResult.Success) {
            try {
                val wordCount = MetadataUtils.countWords(content)
                val metadata = DiaryMetadata(
                    fileUri = file.uri.toString(),
                    wordCount = wordCount,
                    lastModified = file.lastModified(),
                    fileName = file.name ?: "Untitled"
                )
                metadataDao.insertMetadata(metadata)
            } catch (e: Exception) {
                Log.w("DiaryRepository", "Failed to update metadata after save", e)
                // Continue normally - save was successful even if metadata update failed
            }
        }

        return result
    }

    /**
     * NEW: Refresh all metadata (used by pull-to-refresh)
     * Clears cache and recalculates everything
     */
    override suspend fun refreshAllMetadata() {
        withContext(Dispatchers.IO) {
            try {
                // Clear existing cache
                metadataDao.clearAllMetadata()

                // Recalculate metadata for all valid files
                val files = fileManager.getFiles().filter { isFileValid(it) }
                files.forEach { file ->
                    getOrCalculateMetadata(file) // This will cache the new metadata
                }
            } catch (e: Exception) {
                Log.e("DiaryRepository", "Failed to refresh metadata", e)
            }
        }
    }
}
