// /app/src/main/java/com/example/echospace/data/repository/DiaryRepositoryImpl.kt

package com.chani.echospace.data.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
//import com.chani.echospace.data.local.DiaryMetadata
import com.chani.echospace.data.local.DiaryMetadataDao
import com.chani.echospace.data.local.FileManager
import com.chani.echospace.data.local.FileOperationResult
import com.chani.echospace.data.local.MetadataUtils
import com.chani.echospace.domain.model.DiaryEntry
import com.chani.echospace.domain.model.SortOption
import com.chani.echospace.domain.repository.DiaryRepository
import com.chani.echospace.domain.repository.VirtualListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.chani.echospace.data.local.FolderAccessLostException
import com.chani.echospace.utils.PerformanceLogger
import javax.inject.Inject


class DiaryRepositoryImpl @Inject constructor(
    private val fileManager: FileManager,
    private val metadataDao: DiaryMetadataDao,
    private val virtualFolderRepository: VirtualFolderRepository
) : DiaryRepository {

    override suspend fun saveFolderUri(uri: Uri) {
        fileManager.saveFolderUri(uri)
    }

    override suspend fun getEntries(sortOption: SortOption): List<DiaryEntry> {
        return withContext(Dispatchers.IO) {
            try {
                // Cache file list to avoid repeated enumeration
                val files = fileManager.getFiles().filter { isFileValid(it) }

                // Pre-load all metadata in a single batch query instead of individual queries
                val allMetadata = metadataDao.getAllMetadata().associateBy { it.fileUri }

                // Create entries with cached metadata
                val entries = files.map { file ->
                    val metadata = allMetadata[file.uri.toString()]
                    DiaryEntry(
                        file = file,
                        name = file.name?.removeSuffix(".txt") ?: "Untitled",
                        wordCount = metadata?.wordCount,
                        lastModifiedDisplay = metadata?.let {
                            MetadataUtils.formatLastModified(it.lastModified)
                        }
                    )
                }

                // Sort entries
                when (sortOption) {
                    SortOption.NAME_ASC -> entries.sortedBy { it.name.lowercase() }
                    SortOption.NAME_DESC -> entries.sortedByDescending { it.name.lowercase() }
                    SortOption.DATE_NEWEST -> entries.sortedByDescending { it.file.lastModified() }
                    SortOption.DATE_OLDEST -> entries.sortedBy { it.file.lastModified() }
                }
            } catch (e: FolderAccessLostException) {
                throw e
            } catch (e: Exception) {
                Log.e("DiaryRepository", "Error getting entries", e)
                emptyList()
            }
        }
    }

//    /**
//     * NEW: Smart metadata management
//     * Gets cached metadata or calculates if needed
//     */
//    private suspend fun getOrCalculateMetadata(file: DocumentFile): DiaryMetadata? {
//        return try {
//            val fileUri = file.uri.toString()
//            val currentLastModified = file.lastModified()
//
//            // Check if we have up-to-date cached metadata
//            val cachedMetadata = metadataDao.getMetadata(fileUri)
//
//            if (cachedMetadata != null && cachedMetadata.lastModified == currentLastModified) {
//                // Cached metadata is current - return it (fast path)
//                return cachedMetadata
//            }
//
//            // Need to calculate new metadata (slow path, but cached afterward)
//            val content = fileManager.readFile(file)
//            val wordCount = MetadataUtils.countWords(content)
//
//            val newMetadata = DiaryMetadata(
//                fileUri = fileUri,
//                wordCount = wordCount,
//                lastModified = currentLastModified,
//                fileName = file.name ?: "Untitled"
//            )
//
//            // Cache the new metadata for future use
//            metadataDao.insertMetadata(newMetadata)
//            newMetadata
//
//        } catch (e: Exception) {
//            Log.w("DiaryRepository", "Failed to get metadata for ${file.name}", e)
//            null // Graceful fallback - entry will display without metadata
//        }
//    }

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
                // UPDATED: Clean database immediately
                metadataDao.deleteMetadata(entry.file.uri.toString())
                virtualFolderRepository.invalidateCachedPaths(listOf(entry.file.uri.toString()))
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
            try {
                val oldUri = entry.file.uri.toString()
                val newUri = newFile.uri.toString()

                // UPDATED: Use VirtualFolderRepository for comprehensive sync
                virtualFolderRepository.invalidateAndResyncFiles(listOf(oldUri, newUri))
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

        if (result is FileOperationResult.Success) {
            try {
                // UPDATED: Use VirtualFolderRepository for immediate sync
                virtualFolderRepository.invalidateAndResyncFiles(listOf(file.uri.toString()))

            } catch (e: Exception) {
                Log.w("DiaryRepository", "Failed to update metadata after save", e)
                // Continue normally - save was successful even if metadata update failed
            }
        }
        return result
    }

    /**
     * Refresh changed files metadata (used by pull-to-refresh)
     */
    // Enhanced cache clearing
    override suspend fun refreshAllMetadata() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("DiaryRepository", "🔄 Starting comprehensive database refresh")

                // Use VirtualFolderRepository for complete sync
                val syncResult = virtualFolderRepository.syncDatabaseWithFileSystem(forceFullSync = true)

                if (syncResult.success) {
                    Log.d("DiaryRepository", "✅ Refresh completed: ${syncResult.filesProcessed} files processed")
                } else {
                    Log.e("DiaryRepository", "❌ Refresh failed: ${syncResult.error}")
                }

            } catch (e: Exception) {
                Log.e("DiaryRepository", "Failed to refresh metadata", e)
            }
        }
    }

    override suspend fun getVirtualListing(
        pathKeys: List<String>, sort: SortOption
    ): VirtualListing = withContext(Dispatchers.IO) {
        PerformanceLogger.logStep("getVirtualListing started for path: ${pathKeys.joinToString("/")}")

        return@withContext PerformanceLogger.measureAndLog("getVirtualListing total") {
            // NEW APPROACH: Use database-only for display
            PerformanceLogger.measureAndLog("buildVirtualListingFromDatabase") {
                virtualFolderRepository.buildVirtualListingFromDatabase(pathKeys, sort)
            }
        }
    }

    // NEW: Add method to check if database sync is needed
    override suspend fun isDatabaseSyncNeeded(): Boolean {
        return try {
            // Delegate to repository/virtual folder repository
            virtualFolderRepository.isDatabaseSyncNeeded()
        } catch (e: Exception) {
            Log.w("HomeViewModel", "Failed to check sync status: ${e.message}")
            true // Assume sync needed on error
        }
    }


}
