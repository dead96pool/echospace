package com.chani.echospace.data.local

import androidx.room.*

@Dao
interface DiaryMetadataDao {

    /**
     * Get metadata for a specific file
     */
    @Query("SELECT * FROM diary_metadata WHERE fileUri = :fileUri")
    suspend fun getMetadata(fileUri: String): DiaryMetadata?

    /**
     * Insert or update metadata for a file
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: DiaryMetadata)

    /**
     * Delete metadata for a specific file (used when file is deleted)
     */
    @Query("DELETE FROM diary_metadata WHERE fileUri = :fileUri")
    suspend fun deleteMetadata(fileUri: String)

    /**
     * Get all metadata (used for refresh operations)
     */
    @Query("SELECT * FROM diary_metadata")
    suspend fun getAllMetadata(): List<DiaryMetadata>

    /**
     * Clear all metadata (used for full refresh)
     */
    @Query("DELETE FROM diary_metadata")
    suspend fun clearAllMetadata()
}
