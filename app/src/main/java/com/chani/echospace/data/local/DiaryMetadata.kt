

package com.chani.echospace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_metadata")
data class DiaryMetadata(
    @PrimaryKey
    val fileUri: String,
    val wordCount: Int,
    val lastModified: Long,
    val fileName: String,
    val calculatedAt: Long = System.currentTimeMillis()     // last metadata calculation for debugging
)
