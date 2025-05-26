package com.example.echospace.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar


fun countWords(file: File): Int {
    return try {
        val content = file.readText()
        content.split(Regex("\\s+")).count { it.isNotEmpty() }
    } catch (e: Exception) {
        0
    }
}

fun getRelativeTimeText(timestamp: Long): String {
    val now = Calendar.getInstance()
    val fileTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    // Fix: Calculate differences correctly
    val daysDiff = now.get(Calendar.DAY_OF_YEAR) - fileTime.get(Calendar.DAY_OF_YEAR)
    val yearsDiff = now.get(Calendar.YEAR) - fileTime.get(Calendar.YEAR)

    return when {
        yearsDiff == 0 && daysDiff == 0 -> "Today"
        yearsDiff == 0 && daysDiff == 1 -> "Yesterday"
        yearsDiff == 0 && daysDiff in 2..6 -> "$daysDiff days ago"
        yearsDiff == 0 && daysDiff < 30 -> "$daysDiff days ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
