// /app/src/main/java/com/example/echospace/ui/home/HomeViewModel.kt

package com.example.echospace.ui.home

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    fun loadEntries() {
        viewModelScope.launch {
            _entries.value = diaryRepository.getEntries()
        }
    }

    suspend fun createEntry(fileName: String): DocumentFile? {
        return diaryRepository.createEntry(fileName)
    }
}
