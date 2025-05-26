// domain/model/VirtualFolder.kt
package com.example.echospace.domain.model

data class VirtualFolder(
    val name: String,
    val path: String,
    val subfolders: List<VirtualFolder> = emptyList(),
    val entries: List<DiaryEntry> = emptyList()
)
