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
import com.chani.echospace.domain.repository.VirtualFolder
import com.chani.echospace.domain.repository.VirtualListing
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.Locale
import com.chani.echospace.data.local.FolderAccessLostException
import javax.inject.Inject


class DiaryRepositoryImpl @Inject constructor(
    private val fileManager: FileManager,
    private val metadataDao: DiaryMetadataDao
) : DiaryRepository {

    // ---- Virtual index cache (in-memory) ----
    private var virtualIndexBuiltAt: Long = 0L

    // Map of normalized path key chain joined by "/" → aggregated mostRecent timestamp (subtree)
    private val folderMostRecent: MutableMap<String, Long> = mutableMapOf()

    // Map of normalized path key chain joined by "/" → list of files belonging exactly at that node
    private val pathFiles: MutableMap<String, MutableList<DiaryEntry>> = mutableMapOf()

    // Map of normalized path key chain joined by "/" → map of child folderKey → displayName (for leaf names)
    private val pathChildFolders: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    private fun joinKeys(keys: List<String>): String = keys.joinToString("/")

    private fun clearVirtualIndex() {
        folderMostRecent.clear()
        pathFiles.clear()
        pathChildFolders.clear()
        virtualIndexBuiltAt = 0L
    }

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

        // NEW: invalidate virtual index after deletion
        clearVirtualIndex()

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

                // NEW: invalidate virtual index after rename
                clearVirtualIndex()

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
                // NEW: invalidate virtual index after save
                clearVirtualIndex()

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
    override suspend fun refreshAllMetadata() {
        withContext(Dispatchers.IO) {
            try {
                // Get current files and existing metadata
                val files = fileManager.getFiles().filter { isFileValid(it) }
                val existingMetadata = metadataDao.getAllMetadata()
                val existingUris = existingMetadata.associateBy { it.fileUri }

                // Only process files that need updating
                val filesToUpdate = files.filter { file ->
                    val existing = existingUris[file.uri.toString()]
                    existing == null || existing.lastModified != file.lastModified()
                }

                Log.d("DiaryRepository", "Refreshing ${filesToUpdate.size} of ${files.size} files")

                // Process files in parallel (limit concurrency to avoid overwhelming storage)
                filesToUpdate.chunked(5).forEach { chunk ->
                    coroutineScope {
                        chunk.map { file ->
                            async {
                                getOrCalculateMetadata(file)
                            }
                        }.awaitAll()
                    }
                }

                // Clean up metadata for deleted files
                val currentUris = files.map { it.uri.toString() }.toSet()
                val orphanedMetadata = existingUris.keys - currentUris
                orphanedMetadata.forEach { orphanedUri ->
                    metadataDao.deleteMetadata(orphanedUri)
                }

                // NEW: rebuild or invalidate virtual index so folder timestamps reflect changes
                clearVirtualIndex()

            } catch (e: Exception) {
                Log.e("DiaryRepository", "Failed to refresh metadata", e)
            }
        }
    }

    private suspend fun buildVirtualIndex(sortOption: SortOption) {
        // If already built recently in this process and caches exist, we can skip.
        // For simplicity, rebuild on every call from callers that need fresh structure.
        clearVirtualIndex()

        // Get flat entries via existing logic to reuse metadata and avoid duplicate IO
        val flatEntries = getEntries(sortOption)

        for (entry in flatEntries) {
            val file = entry.file
            val content = try {
                fileManager.readFile(file)
            } catch (e: Exception) {
                // If content read fails, skip virtual tagging for this file
                continue
            }

            val paths = MetadataUtils.buildVirtualPathsFromContent(content)
            if (paths.isEmpty()) {
                // No tags: file belongs to virtual root (empty path)
                val key = "" // root node
                pathFiles.getOrPut(key) { mutableListOf() }.add(entry)
                // Update folderMostRecent for root with this file’s lastModified
                val lm = file.lastModified()
                val current = folderMostRecent[key]
                if (current == null || lm > current) folderMostRecent[key] = lm
                continue
            }

            // For each virtual path (can be multiple tag runs)
            for (path in paths) {
                // normalized keys chain from root to leaf
                val keys = path.map { it.key } // lowercased already by parser
                val leafKey = joinKeys(keys)
                // Attach file to leaf bucket
                pathFiles.getOrPut(leafKey) { mutableListOf() }.add(entry)

                // Register child folders along the chain for navigation + display names
                // Also compute subtree most-recent timestamps by propagating from leaf up to root
                val lastModified = file.lastModified()

                // For each node from root → leaf, record child folders and update most-recent
                for (i in keys.indices) {
                    val nodeKeys = keys.subList(0, i + 1)
                    val nodeKey = joinKeys(nodeKeys)
                    // Update most recent
                    val current = folderMostRecent[nodeKey]
                    if (current == null || lastModified > current) {
                        folderMostRecent[nodeKey] = lastModified
                    }
                    // Also update root "" for global sorting/top-level
                    val rootCurrent = folderMostRecent[""]
                    if (rootCurrent == null || lastModified > rootCurrent) {
                        folderMostRecent[""] = lastModified
                    }

                    // Register the child folder name for its parent path
                    val parentKey = if (i == 0) "" else joinKeys(keys.subList(0, i))
                    val childKey = keys[i]
                    val childDisplay = path[i].display // preserve raw display text
                    val map = pathChildFolders.getOrPut(parentKey) { mutableMapOf() }
                    // If same childKey appears with different display casings, keep first seen or update — choose first
                    if (!map.containsKey(childKey)) {
                        map[childKey] = childDisplay
                    }
                }
            }
        }

        // Ensure root node exists in maps to avoid null checks later
        folderMostRecent.putIfAbsent("", 0L)
        pathFiles.putIfAbsent("", mutableListOf())

        virtualIndexBuiltAt = System.currentTimeMillis()
    }

    override suspend fun getVirtualListing(
        pathKeys: List<String>, sort: SortOption
    ): VirtualListing = withContext(Dispatchers.IO) {
        // Rebuild the virtual index each time to keep behavior consistent with refresh/save flows.
        // Later we can optimize by reusing and invalidating on operations.
        buildVirtualIndex(sort)

        val nodeKey = joinKeys(pathKeys)

        // Child folders for this node
        val children = pathChildFolders[nodeKey].orEmpty()

        val folderItems = children.map { (childKey, displayName) ->
            val fullKeys = if (nodeKey.isEmpty()) listOf(childKey) else (pathKeys + childKey)
            val fullKeyJoined = joinKeys(fullKeys)
            val mostRecent = folderMostRecent[fullKeyJoined] ?: 0L
            VirtualFolder(
                nameDisplay = displayName,
                nameKey = childKey,
                fullPathKeys = fullKeys,
                mostRecentMillis = mostRecent
            )
        }

        // Files at this node (exact leaf matches)
        val filesAtNode = pathFiles[nodeKey].orEmpty()
            .distinctBy { it.file.uri.toString() } // ← Add this line
            .toList()

        // Apply sorting:
        // - Folders are sorted using the same SortOption, where date means by mostRecentMillis.
        // - Files are sorted using existing SortOption semantics (same as getEntries).
        val sortedFolders = when (sort) {
            SortOption.NAME_ASC -> folderItems.sortedBy { it.nameDisplay.lowercase(Locale.getDefault()) }
            SortOption.NAME_DESC -> folderItems.sortedByDescending { it.nameDisplay.lowercase(Locale.getDefault()) }
            SortOption.DATE_NEWEST -> folderItems.sortedByDescending { it.mostRecentMillis }
            SortOption.DATE_OLDEST -> folderItems.sortedBy { it.mostRecentMillis }
        }

        val sortedFiles = when (sort) {
            SortOption.NAME_ASC -> filesAtNode.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SortOption.NAME_DESC -> filesAtNode.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            SortOption.DATE_NEWEST -> filesAtNode.sortedByDescending { it.file.lastModified() }
            SortOption.DATE_OLDEST -> filesAtNode.sortedBy { it.file.lastModified() }
        }

        VirtualListing(
            folders = sortedFolders,
            files = sortedFiles
        )
    }


}
