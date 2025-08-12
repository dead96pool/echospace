package com.chani.echospace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "virtual_paths")
@TypeConverters(VirtualPathConverters::class)
data class VirtualPathEntity(
    @PrimaryKey val fileUri: String,
    val fileName: String,
    val lastModified: Long,
    val virtualPaths: String, // Store as delimited string instead of complex JSON
    val createdAt: Long = System.currentTimeMillis()
)

// Simplified data class for Room storage
data class VirtualSegmentData(
    val key: String,
    val display: String
)

// Type converters using simple string serialization (no Gson needed)
class VirtualPathConverters {

    // Convert List<List<VirtualSegmentData>> to String
    @TypeConverter
    fun fromVirtualPaths(paths: List<List<VirtualSegmentData>>): String {
        if (paths.isEmpty()) return ""

        return paths.joinToString(separator = "||") { path ->
            path.joinToString(separator = "|") { segment ->
                "${segment.key}:${segment.display}"
            }
        }
    }

    // Convert String back to List<List<VirtualSegmentData>>
    @TypeConverter
    fun toVirtualPaths(pathsString: String): List<List<VirtualSegmentData>> {
        if (pathsString.isEmpty()) return emptyList()

        return try {
            pathsString.split("||").map { pathString ->
                pathString.split("|").mapNotNull { segmentString ->
                    val parts = segmentString.split(":", limit = 2)
                    if (parts.size == 2) {
                        VirtualSegmentData(key = parts[0], display = parts[1])
                    } else {
                        null
                    }
                }
            }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
