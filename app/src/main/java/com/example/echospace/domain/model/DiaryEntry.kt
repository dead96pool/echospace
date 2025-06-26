// /app/src/main/java/com/example/echospace/domain/model/DiaryEntry.kt

package com.example.echospace.domain.model

import androidx.documentfile.provider.DocumentFile

data class DiaryEntry(
    val file: DocumentFile,
    val name: String
)
