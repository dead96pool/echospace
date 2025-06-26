//utils/SortPreferences.kt:
package com.example.echospace.utils

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.sortDataStore by preferencesDataStore("sort_preferences")

