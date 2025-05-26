package com.example.echospace.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun countWords(file: File): Int {
    return try {
        val content = file.readText()
        content.split(Regex("\\s+")).count { it.isNotEmpty() }
    } catch (e: Exception) {
        0
    }
}

fun getRelativeTimeText(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 24 * 60 * 60 * 1000 -> "Today"
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
