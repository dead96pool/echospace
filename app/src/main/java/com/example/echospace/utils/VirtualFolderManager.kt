// utils/VirtualFolderManager.kt
package com.example.echospace.utils

import androidx.compose.runtime.Stable
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.FolderItem

@Stable
class VirtualFolderManager {
    // Cache for computed folder contents
    private var cachedContents: Map<String, List<FolderItem>> = emptyMap()
    private var lastEntriesHash: Int = 0

    fun getFolderContents(
        entries: List<DiaryEntry>,
        currentPath: String
    ): List<FolderItem> {
        val entriesHash = entries.hashCode()

        // Return cached result if entries haven't changed
        if (entriesHash == lastEntriesHash && cachedContents.containsKey(currentPath)) {
            return cachedContents[currentPath] ?: emptyList()
        }

        // Recompute only if entries changed
        if (entriesHash != lastEntriesHash) {
            cachedContents = computeAllFolderContents(entries)
            lastEntriesHash = entriesHash
        }

        return cachedContents[currentPath] ?: emptyList()
    }

    private fun computeAllFolderContents(entries: List<DiaryEntry>): Map<String, List<FolderItem>> {
        val result = mutableMapOf<String, List<FolderItem>>()

        // Pre-compute all possible paths
        val allPaths = mutableSetOf<String>()
        allPaths.add("") // Root path

        entries.forEach { entry ->
            entry.tags.forEach { tagPath ->
                val parts = tagPath.split("#")
                for (i in 1..parts.size) {
                    allPaths.add(parts.take(i).joinToString("#"))
                }
            }
        }

        // Compute contents for each path
        allPaths.forEach { path ->
            result[path] = computeFolderContentsForPath(entries, path)
        }

        return result
    }

    private fun computeFolderContentsForPath(
        entries: List<DiaryEntry>,
        currentPath: String
    ): List<FolderItem> {
        val items = mutableListOf<FolderItem>()
        val fileMap = mutableMapOf<String, MutableSet<DiaryEntry>>()
        val subfolderSet = mutableSetOf<String>()

        entries.forEach { entry ->
            entry.tags.forEach { tagPath ->
                val parts = tagPath.split("#")

                if (currentPath.isEmpty()) {
                    // ROOT: collect only first‐level folders
                    subfolderSet.add(parts[0])
                } else {
                    // INSIDE a folder: currentPathParts = ["test"] for path "test"
                    val prefix = currentPath.split("#")
                    if (parts.size >= prefix.size && parts.take(prefix.size) == prefix) {
                        if (parts.size == prefix.size) {
                            // tag exactly equals current folder: show file here
                            fileMap.getOrPut(tagPath) { mutableSetOf() }.add(entry)
                        } else {
                            // deeper tag => show next subfolder name
                            subfolderSet.add(parts[prefix.size])
                        }
                    }
                }
            }
        }

        // 1. Untagged files at root
        if (currentPath.isEmpty()) {
            entries.filter { it.tags.isEmpty() }
                .forEach { items.add(FolderItem(it.title, "", false, it)) }
        }

        // 2. Add folder items
        subfolderSet.sorted().forEach { name ->
            val path = if (currentPath.isEmpty()) name else "$currentPath#$name"
            items.add(FolderItem(name, path, true))
        }

        // 3. Add file items in this folder
        fileMap.forEach { (tagPath, set) ->
            set.forEach { e ->
                items.add(FolderItem(e.title, tagPath, false, e))
            }
        }

        // 4. Sort: folders first, then files
        return items.sortedWith(
            compareBy<FolderItem> { !it.isFolder }
                .thenBy { it.name }
        )
    }

    fun getParentPath(currentPath: String): String? {
        if (currentPath.isEmpty()) return null
        val parts = currentPath.split("#")
        return if (parts.size == 1) "" else parts.dropLast(1).joinToString("#")
    }

    fun findNearestParentWithContent(
        currentPath: String,
        entries: List<DiaryEntry>
    ): String {
        if (currentPath.isEmpty()) return ""

        val pathParts = currentPath.split("#")

        // Check each parent level from current up to root
        for (i in pathParts.size - 1 downTo 0) {
            val parentPath = if (i == 0) "" else pathParts.take(i).joinToString("#")

            if (hasContentInPath(parentPath, entries)) {
                return parentPath
            }
        }

        return "" // Return to root if no parent has content
    }

    fun hasContentInPath(path: String, entries: List<DiaryEntry>): Boolean {
        if (path.isEmpty()) {
            // Root has content if there are untagged files or any tagged files
            return entries.isNotEmpty()
        }

        val pathParts = path.split("#")

        return entries.any { entry ->
            entry.tags.any { tagPath ->
                val tagParts = tagPath.split("#")
                // Check if this tag starts with our path
                tagParts.size >= pathParts.size &&
                        tagParts.take(pathParts.size) == pathParts
            }
        }
    }

    // Optional: Clear cache manually if needed
    fun clearCache() {
        cachedContents = emptyMap()
        lastEntriesHash = 0
    }
}
