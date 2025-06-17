// /app/src/main/java/com/example/echospace/ui/editor/EditorViewModel.kt
package com.example.echospace.ui.editor

import androidx.lifecycle.ViewModel
import com.example.echospace.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    val diaryRepository: DiaryRepository
) : ViewModel()
