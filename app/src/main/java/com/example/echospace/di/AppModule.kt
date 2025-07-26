// /app/src/main/java/com/example/echospace/di/AppModule.kt

package com.example.echospace.di

import android.content.Context
import androidx.room.Room
import com.example.echospace.data.local.EchoSpaceDatabase
import com.example.echospace.data.local.DiaryMetadataDao
import com.example.echospace.data.local.FileManager
import com.example.echospace.data.preferences.ThemePreferences
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.domain.repository.DiaryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFileManager(@ApplicationContext context: Context): FileManager {
        return FileManager(context)
    }

    @Provides
    @Singleton
    fun provideDiaryRepository(
        fileManager: FileManager,
        metadataDao: DiaryMetadataDao
    ): DiaryRepository {
        return DiaryRepositoryImpl(fileManager, metadataDao)
    }

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences {
        return ThemePreferences(context)
    }

    /**
     * Provides the Room database instance
     */
    @Provides
    @Singleton
    fun provideEchoSpaceDatabase(@ApplicationContext context: Context): EchoSpaceDatabase {
        return Room.databaseBuilder(
            context,
            EchoSpaceDatabase::class.java,
            EchoSpaceDatabase.DATABASE_NAME
        ).build()
    }

    /**
     * Provides the DiaryMetadataDao from the database
     */
    @Provides
    fun provideDiaryMetadataDao(database: EchoSpaceDatabase): DiaryMetadataDao {
        return database.diaryMetadataDao()
    }
}
