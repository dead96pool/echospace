// utils/VirtualFolderManager.kt
package com.example.echospace.utils

import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.FolderItem

class VirtualFolderManager {

    fun getFolderContents(
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
}
