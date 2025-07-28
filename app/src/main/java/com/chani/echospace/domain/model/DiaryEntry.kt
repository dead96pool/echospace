// /app/src/main/java/com/example/echospace/domain/model/DiaryEntry.kt

package com.chani.echospace.domain.model

import androidx.documentfile.provider.DocumentFile

data class DiaryEntry(
    val file: DocumentFile,
    val name: String,
    val wordCount: Int? = null,
    val lastModifiedDisplay: String? = null
)
