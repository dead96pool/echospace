// domain/model/DiaryEntry.kt
package com.example.echospace.domain.model

//import java.io.File
import androidx.documentfile.provider.DocumentFile

data class DiaryEntry(
    val file: DocumentFile,
    val title: String,
    val lastModified: Long,
    val tags: List<String> = emptyList()
) {

    companion object {
        fun fromFile(file: DocumentFile): DiaryEntry {
            val title = file.name?.removeSuffix(".txt") ?: "Untitled"
            val lastModified = file.lastModified()

            return DiaryEntry(file, title, lastModified)
        }
    }
}
