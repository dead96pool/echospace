package com.chani.echospace.data.repository

import android.content.Context
//import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.chani.echospace.data.local.*
import com.chani.echospace.domain.model.DiaryEntry
import com.chani.echospace.domain.model.SortOption
import com.chani.echospace.domain.repository.VirtualFolder
import com.chani.echospace.domain.repository.VirtualListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.chani.echospace.utils.PerformanceLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

data class DatabaseSyncResult(
    val success: Boolean,
    val filesProcessed: Int = 0,
    val newFiles: Int = 0,
    val updatedFiles: Int = 0,
    val deletedFiles: Int = 0,
    val syncTimeMs: Long = 0,
    val error: String? = null
)

@Singleton
class VirtualFolderRepository @Inject constructor(
    private val virtualPathDao: VirtualPathDao,
    private val metadataDao: DiaryMetadataDao,
    private val fileManager: FileManager,
    @ApplicationContext private val context: Context
) {

    // Convert VirtualPath (domain) to VirtualSegmentData (database)
    private fun VirtualPath.toSegmentDataList(): List<VirtualSegmentData> {
        return this.map { segment ->
            VirtualSegmentData(key = segment.key, display = segment.display)
        }
    }

    // Convert VirtualSegmentData (database) to VirtualPath (domain)
    private fun List<VirtualSegmentData>.toVirtualPath(): VirtualPath {
        return this.map { segmentData ->
            VirtualSegment(key = segmentData.key, display = segmentData.display)
        }
    }

    // Get or compute virtual paths for a file (with Room caching)
    private suspend fun getVirtualPathsForFile(entry: DiaryEntry): List<VirtualPath> = withContext(Dispatchers.IO) {
        val fileUri = entry.file.uri.toString()
        val currentModified = entry.file.lastModified()

        // Check if we have cached paths
        val cachedEntity = virtualPathDao.getVirtualPathsByFileUri(fileUri)

        if (cachedEntity != null && cachedEntity.lastModified == currentModified) {
            // Return cached paths (super fast)
            Log.d("VirtualFolderRepo", "✅ Using cached paths for ${entry.name}")
            return@withContext cachedEntity.virtualPaths.let { pathsString ->
                VirtualPathConverters().toVirtualPaths(pathsString).map { it.toVirtualPath() }
            }
        }

        // Compute new paths (only when file changed)
        Log.d("VirtualFolderRepo", "🔄 Computing new paths for ${entry.name}")
        val content = try {
            fileManager.readFile(entry.file)
        } catch (e: Exception) {
            Log.e("VirtualFolderRepo", "Failed to read file ${entry.name}: ${e.message}")
            return@withContext emptyList()
        }

        val newPaths = MetadataUtils.buildVirtualPathsFromContent(content)

        // Cache the new paths in Room database
        val pathsAsSegmentData = newPaths.map { it.toSegmentDataList() }
        val pathsString = VirtualPathConverters().fromVirtualPaths(pathsAsSegmentData)

        val entity = VirtualPathEntity(
            fileUri = fileUri,
            fileName = entry.name,
            lastModified = currentModified,
            virtualPaths = pathsString
        )

        virtualPathDao.insertVirtualPath(entity)
        Log.d("VirtualFolderRepo", "💾 Cached ${newPaths.size} paths for ${entry.name}")

        return@withContext newPaths
    }

    // NEW: Build virtual listing entirely from database (no file system access)
    suspend fun buildVirtualListingFromDatabase(
        currentPathKeys: List<String>,
        sortOption: SortOption
    ): VirtualListing = withContext(Dispatchers.IO) {

        Log.d("VirtualFolderRepo", "🗃️ Building listing from database for path: ${currentPathKeys.joinToString("/")}")

        // Get all cached virtual paths from database
        val allVirtualPaths = PerformanceLogger.measureAndLog("getAllVirtualPaths from DB") {
            virtualPathDao.getAllVirtualPaths()
        }

        // Get all metadata from database
        val allMetadata = PerformanceLogger.measureAndLog("getAllMetadata from DB") {
            metadataDao.getAllMetadata()
        }

        // Create DiaryEntry objects from database data only
        val allEntries = allMetadata.mapNotNull { metadata ->
            try {
                // Create DocumentFile from URI (lightweight operation)
                val uri = metadata.fileUri.toUri()
                val documentFile = DocumentFile.fromSingleUri(context, uri)

                if (documentFile?.exists() == true) {
                    DiaryEntry(
                        file = documentFile,
                        name = metadata.fileName.removeSuffix(".txt"),
                        wordCount = metadata.wordCount,
                        lastModifiedDisplay = MetadataUtils.formatLastModified(metadata.lastModified)
                    )
                } else {
                    null // File no longer exists - will be cleaned up later
                }
            } catch (e: Exception) {
                Log.w("VirtualFolderRepo", "Failed to create entry for ${metadata.fileUri}: ${e.message}")
                null
            }
        }

        // Build virtual structure using cached virtual paths
        val folderMostRecent: MutableMap<String, Long> = mutableMapOf()
        val pathFiles: MutableMap<String, MutableList<DiaryEntry>> = mutableMapOf()
        val pathChildFolders: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

        fun joinKeys(keys: List<String>): String = keys.joinToString("/")

        // Process virtual paths from database
        for (virtualPathEntity in allVirtualPaths) {
            val entry = allEntries.find { it.file.uri.toString() == virtualPathEntity.fileUri }
            if (entry == null) continue

            val virtualPaths = VirtualPathConverters().toVirtualPaths(virtualPathEntity.virtualPaths)
                .map { it.toVirtualPath() }

            if (virtualPaths.isEmpty()) {
                // Root file
                val key = ""
                pathFiles.getOrPut(key) { mutableListOf() }.add(entry)
                val current = folderMostRecent[key]
                if (current == null || virtualPathEntity.lastModified > current) {
                    folderMostRecent[key] = virtualPathEntity.lastModified
                }
            } else {
                // Virtual paths
                for (path in virtualPaths) {
                    val keys = path.map { it.key }
                    val leafKey = joinKeys(keys)
                    pathFiles.getOrPut(leafKey) { mutableListOf() }.add(entry)

                    // Update folder timestamps and register child folders
                    for (i in keys.indices) {
                        val nodeKeys = keys.subList(0, i + 1)
                        val nodeKey = joinKeys(nodeKeys)

                        val current = folderMostRecent[nodeKey]
                        if (current == null || virtualPathEntity.lastModified > current) {
                            folderMostRecent[nodeKey] = virtualPathEntity.lastModified
                        }

                        val rootCurrent = folderMostRecent[""]
                        if (rootCurrent == null || virtualPathEntity.lastModified > rootCurrent) {
                            folderMostRecent[""] = virtualPathEntity.lastModified
                        }

                        val parentKey = if (i == 0) "" else joinKeys(keys.subList(0, i))
                        val childKey = keys[i]
                        val childDisplay = path[i].display
                        val map = pathChildFolders.getOrPut(parentKey) { mutableMapOf() }
                        if (!map.containsKey(childKey)) {
                            map[childKey] = childDisplay
                        }
                    }
                }
            }
        }

        // Build result for current path
        val nodeKey = joinKeys(currentPathKeys)
        val children = pathChildFolders[nodeKey].orEmpty()

        val folderItems = children.map { (childKey, displayName) ->
            val fullKeys = if (nodeKey.isEmpty()) listOf(childKey) else (currentPathKeys + childKey)
            val fullKeyJoined = joinKeys(fullKeys)
            val mostRecent = folderMostRecent[fullKeyJoined] ?: 0L
            VirtualFolder(
                nameDisplay = displayName,
                nameKey = childKey,
                fullPathKeys = fullKeys,
                mostRecentMillis = mostRecent
            )
        }

        val filesAtNode = pathFiles[nodeKey].orEmpty()
            .distinctBy { it.file.uri.toString() }
            .toList()

        // Apply sorting
        val sortedFolders = when (sortOption) {
            SortOption.NAME_ASC -> folderItems.sortedBy { it.nameDisplay.lowercase(Locale.getDefault()) }
            SortOption.NAME_DESC -> folderItems.sortedByDescending { it.nameDisplay.lowercase(Locale.getDefault()) }
            SortOption.DATE_NEWEST -> folderItems.sortedByDescending { it.mostRecentMillis }
            SortOption.DATE_OLDEST -> folderItems.sortedBy { it.mostRecentMillis }
        }

        val sortedFiles = when (sortOption) {
            SortOption.NAME_ASC -> filesAtNode.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SortOption.NAME_DESC -> filesAtNode.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            SortOption.DATE_NEWEST -> filesAtNode.sortedByDescending { it.file.lastModified() }
            SortOption.DATE_OLDEST -> filesAtNode.sortedBy { it.file.lastModified() }
        }

        Log.d("VirtualFolderRepo", "✅ Built from DB: ${sortedFolders.size} folders, ${sortedFiles.size} files")

        return@withContext VirtualListing(
            folders = sortedFolders,
            files = sortedFiles
        )
    }

    // NEW: Complete database synchronization with file system
    suspend fun syncDatabaseWithFileSystem(forceFullSync: Boolean = false): DatabaseSyncResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("VirtualFolderRepo", "🔄 Starting database sync with file system")
                val startTime = System.currentTimeMillis()

                // Get current files from SAF
                val currentFiles = PerformanceLogger.measureAndLog("Get files from SAF") {
                    fileManager.getFiles().filter {
                        try {
                            it.exists() && it.canRead() && (it.name?.endsWith(".txt") == true)
                        } catch (e: Exception) {
                            Log.w("VirtualFolderRepo", "Failed to validate file ${it.name}: ${e.message}")
                            false
                        }
                    }
                }

                // Get existing database entries
                val existingMetadata = metadataDao.getAllMetadata()
                val existingVirtualPaths = virtualPathDao.getAllVirtualPaths()

                val metadataByUri = existingMetadata.associateBy { it.fileUri }
                val virtualPathsByUri = existingVirtualPaths.associateBy { it.fileUri }

                var newFiles = 0
                var updatedFiles = 0
                var deletedFiles = 0

                // Process current files
                for (file in currentFiles) {
                    val fileUri = file.uri.toString()
                    val currentModified = file.lastModified()

                    val existingMeta = metadataByUri[fileUri]
                    val existingVPath = virtualPathsByUri[fileUri]

                    // Check if file needs processing
                    val needsMetadataUpdate = existingMeta == null ||
                            existingMeta.lastModified != currentModified || forceFullSync
                    val needsVirtualPathUpdate = existingVPath == null ||
                            existingVPath.lastModified != currentModified || forceFullSync

                    if (needsMetadataUpdate || needsVirtualPathUpdate) {
                        try {
                            // Create DiaryEntry for processing
                            val entry = DiaryEntry(
                                file = file,
                                name = file.name?.removeSuffix(".txt") ?: "Untitled"
                            )

                            // Update metadata if needed
                            if (needsMetadataUpdate) {
                                val content = fileManager.readFile(file)
                                val wordCount = MetadataUtils.countWords(content)

                                val metadata = DiaryMetadata(
                                    fileUri = fileUri,
                                    wordCount = wordCount,
                                    lastModified = currentModified,
                                    fileName = file.name ?: "Untitled"
                                )

                                metadataDao.insertMetadata(metadata)

                                if (existingMeta == null) newFiles++ else updatedFiles++
                            }

                            // Update virtual paths if needed
                            if (needsVirtualPathUpdate) {
                                // This will compute and cache new virtual paths
                                getVirtualPathsForFile(entry)
                            }

                        } catch (e: Exception) {
                            Log.e("VirtualFolderRepo", "Failed to sync file ${file.name}: ${e.message}")
                        }
                    }
                }

                // Clean up deleted files
                val currentUris = currentFiles.map { it.uri.toString() }.toSet()

                // Remove orphaned metadata
                val orphanedMetadata = existingMetadata.filter { it.fileUri !in currentUris }
                for (orphaned in orphanedMetadata) {
                    metadataDao.deleteMetadata(orphaned.fileUri)
                    deletedFiles++
                }

                // Remove orphaned virtual paths
                val orphanedVirtualPaths = existingVirtualPaths.filter { it.fileUri !in currentUris }
                if (orphanedVirtualPaths.isNotEmpty()) {
                    virtualPathDao.deleteVirtualPathsByFileUris(orphanedVirtualPaths.map { it.fileUri })
                }

                val syncTime = System.currentTimeMillis() - startTime
                Log.d("VirtualFolderRepo", "✅ Database sync completed in ${syncTime}ms: $newFiles new, $updatedFiles updated, $deletedFiles deleted")

                DatabaseSyncResult(
                    success = true,
                    filesProcessed = currentFiles.size,
                    newFiles = newFiles,
                    updatedFiles = updatedFiles,
                    deletedFiles = deletedFiles,
                    syncTimeMs = syncTime
                )

            } catch (e: Exception) {
                Log.e("VirtualFolderRepo", "Failed to sync database: ${e.message}", e)
                DatabaseSyncResult(success = false, error = e.message)
            }
        }
    }

    // NEW: Check if database needs sync (lightweight check)
    suspend fun isDatabaseSyncNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileCount = fileManager.getFiles().filter {
                    it.exists() && it.name?.endsWith(".txt") == true
                }.size
                val metadataCount = metadataDao.getAllMetadata().size

                // If counts don't match, sync is needed
                fileCount != metadataCount

            } catch (e: Exception) {
                Log.w("VirtualFolderRepo", "Failed to check sync status: ${e.message}")
                true // Assume sync needed on error
            }
        }
    }

    // NEW: Invalidate specific files and trigger resync
    suspend fun invalidateAndResyncFiles(fileUris: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                // Remove from cache
                invalidateCachedPaths(fileUris)

                // Get current files that match the URIs
                val currentFiles = fileManager.getFiles().filter {
                    it.uri.toString() in fileUris && it.exists()
                }

                // Resync these specific files
                for (file in currentFiles) {
                    val entry = DiaryEntry(file, file.name?.removeSuffix(".txt") ?: "Untitled")

                    // Update metadata
                    val content = fileManager.readFile(file)
                    val wordCount = MetadataUtils.countWords(content)
                    val metadata = DiaryMetadata(
                        fileUri = file.uri.toString(),
                        wordCount = wordCount,
                        lastModified = file.lastModified(),
                        fileName = file.name ?: "Untitled"
                    )
                    metadataDao.insertMetadata(metadata)

                    // Update virtual paths
                    getVirtualPathsForFile(entry)
                }

                Log.d("VirtualFolderRepo", "✅ Resynced ${currentFiles.size} files")

            } catch (e: Exception) {
                Log.e("VirtualFolderRepo", "Failed to resync files: ${e.message}")
            }
        }
    }

    // Clean up stale virtual path entries
    suspend fun cleanupStaleEntries(currentFiles: List<DiaryEntry>) {
        withContext(Dispatchers.IO) {
            try {
                val currentUris = currentFiles.map { it.file.uri.toString() }.toSet()
                val allCachedPaths = virtualPathDao.getAllVirtualPaths()
                val staleUris = allCachedPaths.filter { it.fileUri !in currentUris }.map { it.fileUri }

                if (staleUris.isNotEmpty()) {
                    virtualPathDao.deleteVirtualPathsByFileUris(staleUris)
                    Log.d("VirtualFolderRepo", "🗑️ Cleaned up ${staleUris.size} stale virtual path entries")
                }
                // ADD THIS: Explicit return to make it clear this function returns Unit
                return@withContext
            } catch (e: Exception) {
                Log.e("VirtualFolderRepo", "Failed to cleanup stale entries: ${e.message}")
                // ADD THIS: Explicit return in catch block too
                return@withContext
            }
        }
    }

    // Invalidate cached paths for specific files
    suspend fun invalidateCachedPaths(fileUris: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                virtualPathDao.deleteVirtualPathsByFileUris(fileUris)
                Log.d("VirtualFolderRepo", "🗑️ Invalidated cached paths for ${fileUris.size} files")
            } catch (e: Exception) {
                Log.e("VirtualFolderRepo", "Failed to invalidate cached paths: ${e.message}")
            }
        }
    }

    // Get cached virtual path count (for debugging/monitoring)
    suspend fun getCachedPathCount(): Int {
        return try {
            virtualPathDao.getVirtualPathCount()
        } catch (e: Exception) {
            Log.e("VirtualFolderRepo", "Failed to get cached path count: ${e.message}")
            0
        }
    }

    // Clear all cached virtual paths (for full refresh scenarios)
    suspend fun clearAllCachedPaths() {
        withContext(Dispatchers.IO) {
            try {
                virtualPathDao.deleteAllVirtualPaths()
                Log.d("VirtualFolderRepo", "🗑️ Cleared all cached virtual paths")
            } catch (e: Exception) {
                Log.e("VirtualFolderRepo", "Failed to clear cached paths: ${e.message}")
            }
        }
    }

}
