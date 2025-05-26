// domain/model/DiaryEntry.kt
package com.example.echospace.domain.model

import java.io.File

data class DiaryEntry(
    val file: File,
    val title: String,
    val lastModified: Long,
    val tags: List<String> = emptyList()
) {

    companion object {
        fun fromFile(file: File): DiaryEntry {
            val title = file.nameWithoutExtension
            val lastModified = file.lastModified()
            val tags = parseTagsFromFile(file)

            return DiaryEntry(file, title, lastModified, tags)
        }

        private fun parseTagsFromFile(file: File): List<String> {
            return try {
                val content = file.readText()
                val lines = content.lines().filter { it.isNotBlank() }

                if (lines.isEmpty()) return emptyList()

                val lastLine = lines.last().trim()
                if (lastLine.startsWith("#")) {
                    // Parse tag paths like "#test" or "#testing1#test #test"
                    lastLine.split(" ")
                        .filter { it.startsWith("#") && it.length > 1 }
                        .map { it.substring(1) } // Remove the # prefix
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
