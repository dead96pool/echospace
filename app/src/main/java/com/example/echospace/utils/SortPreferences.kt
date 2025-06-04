//utils/SortPreferences.kt:
package com.example.echospace.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.echospace.domain.repository.SortType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sortDataStore by preferencesDataStore("sort_preferences")

class SortPreferences(private val context: Context) {
    private val SORT_TYPE_KEY = stringPreferencesKey("sort_type")

    val sortType: Flow<SortType> = context.sortDataStore.data.map { preferences ->
        val sortString = preferences[SORT_TYPE_KEY] ?: SortType.NAME_ASC.name
        SortType.valueOf(sortString)
    }

    suspend fun setSortType(sortType: SortType) {
        context.sortDataStore.edit { preferences ->
            preferences[SORT_TYPE_KEY] = sortType.name
        }
    }
}
