// domain/model/FolderItem.kt
package com.example.echospace.domain.model

import androidx.compose.runtime.Stable

@Stable
data class FolderItem(
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val entry: DiaryEntry? = null
)
