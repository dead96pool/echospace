// domain/model/FolderItem.kt
package com.example.echospace.domain.model

data class FolderItem(
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val entry: DiaryEntry? = null
)
