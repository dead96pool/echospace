package com.chani.echospace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DiaryMetadata::class],
    version = 1,
    exportSchema = false
)
abstract class EchoSpaceDatabase : RoomDatabase() {

    /**
     * Provides access to DiaryMetadataDao
     */
    abstract fun diaryMetadataDao(): DiaryMetadataDao

    companion object {
        const val DATABASE_NAME = "echospace_database"
    }
}
