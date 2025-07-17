package com.example.echospace.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.echospace.domain.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

@Singleton
class ThemePreferences @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val SORT_OPTION = stringPreferencesKey("sort_option")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_THEME] ?: false
    }

    val sortOption: Flow<SortOption> = context.dataStore.data.map { preferences ->
        val sortString = preferences[PreferencesKeys.SORT_OPTION] ?: SortOption.DATE_NEWEST.name
        try {
            SortOption.valueOf(sortString)
        } catch (e: IllegalArgumentException) {
            SortOption.DATE_NEWEST // Default fallback
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = isDark
        }
    }

    suspend fun setSortOption(sortOption: SortOption) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_OPTION] = sortOption.name
        }
    }
}
