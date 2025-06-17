// /app/src/main/java/com/example/echospace/ui/home/HomeViewModel.kt
package com.example.echospace.ui.home

import androidx.lifecycle.ViewModel
import com.example.echospace.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    val diaryRepository: DiaryRepository
) : ViewModel()
