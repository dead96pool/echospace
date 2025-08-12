package com.chani.echospace.data.local

import androidx.room.*

@Dao
interface VirtualPathDao {

    /**
     * Get all virtual paths from the database
     */
    @Query("SELECT * FROM virtual_paths")
    suspend fun getAllVirtualPaths(): List<VirtualPathEntity>

    /**
     * Get virtual paths for a specific file URI
     */
    @Query("SELECT * FROM virtual_paths WHERE fileUri = :fileUri")
    suspend fun getVirtualPathsByFileUri(fileUri: String): VirtualPathEntity?

    /**
     * Get virtual paths for multiple file URIs
     */
    @Query("SELECT * FROM virtual_paths WHERE fileUri IN (:fileUris)")
    suspend fun getVirtualPathsByFileUris(fileUris: List<String>): List<VirtualPathEntity>

    /**
     * Insert or replace a virtual path entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVirtualPath(virtualPath: VirtualPathEntity)

    /**
     * Insert or replace multiple virtual path entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVirtualPaths(virtualPaths: List<VirtualPathEntity>)

    /**
     * Delete a specific virtual path entry
     */
    @Delete
    suspend fun deleteVirtualPath(virtualPath: VirtualPathEntity)

    /**
     * Delete virtual path by file URI
     */
    @Query("DELETE FROM virtual_paths WHERE fileUri = :fileUri")
    suspend fun deleteVirtualPathByFileUri(fileUri: String)

    /**
     * Delete virtual paths for multiple file URIs
     */
    @Query("DELETE FROM virtual_paths WHERE fileUri IN (:fileUris)")
    suspend fun deleteVirtualPathsByFileUris(fileUris: List<String>)

    /**
     * Delete all virtual paths (for cleanup)
     */
    @Query("DELETE FROM virtual_paths")
    suspend fun deleteAllVirtualPaths()

    /**
     * Get count of virtual path entries
     */
    @Query("SELECT COUNT(*) FROM virtual_paths")
    suspend fun getVirtualPathCount(): Int

    /**
     * Get virtual paths that were cached before a specific timestamp (for cleanup)
     */
    @Query("SELECT * FROM virtual_paths WHERE createdAt < :timestamp")
    suspend fun getVirtualPathsOlderThan(timestamp: Long): List<VirtualPathEntity>

    /**
     * Delete virtual paths older than a specific timestamp
     */
    @Query("DELETE FROM virtual_paths WHERE createdAt < :timestamp")
    suspend fun provideVirtualPathDao(timestamp: Long)
}
