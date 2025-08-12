package com.chani.echospace.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

@Database(
    entities = [
        DiaryMetadata::class,
        VirtualPathEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(VirtualPathConverters::class)
abstract class EchoSpaceDatabase : RoomDatabase() {

    /**
     * Provides access to DiaryMetadataDao
     */
    abstract fun diaryMetadataDao(): DiaryMetadataDao
    abstract fun virtualPathDao(): VirtualPathDao

    companion object {
        @Volatile
        private var INSTANCE: EchoSpaceDatabase? = null

        fun getDatabase(context: Context): EchoSpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EchoSpaceDatabase::class.java,
                    "echospace_database"
                )
                    .fallbackToDestructiveMigration() // Keep this for now during development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
