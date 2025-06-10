// domain/model/DiaryEntry.kt
package com.example.echospace.domain.model

//import java.io.File
import androidx.compose.runtime.Stable
import androidx.documentfile.provider.DocumentFile

@Stable
data class DiaryEntry(
    val file: DocumentFile,
    val title: String,
    val lastModified: Long,
    val tags: List<String> = emptyList()
)
