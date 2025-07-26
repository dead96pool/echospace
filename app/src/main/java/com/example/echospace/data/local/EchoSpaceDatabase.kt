package com.example.echospace.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

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
