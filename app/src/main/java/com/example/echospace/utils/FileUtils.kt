package com.example.echospace.utils

import android.content.Context
//import java.io.File
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

private val titlePattern = Regex("^#\\s.+?\\s\\d{1,2}:\\d{2}\\s(?:AM|PM)$", RegexOption.IGNORE_CASE)

fun countWords(documentFile: DocumentFile, context: Context): Int {
    return try {
        val content = context.contentResolver.openInputStream(documentFile.uri)?.use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        } ?: ""

        val allLines = content.lines()
        if (allLines.isEmpty()) return 0

        val contentLines = allLines.toMutableList()

        // Remove title line if present
        if (contentLines.isNotEmpty()) {
            val firstLineTrimmed = contentLines.first().trim()
            if (titlePattern.matches(firstLineTrimmed)) {
                contentLines.removeAt(0) // Remove the identified title line
            }
        }

        // Remove hashtag lines at end
        if (contentLines.isNotEmpty()) {
            var lastNonBlankLineIndex = -1
            for (i in contentLines.indices.reversed()) {
                if (contentLines[i].isNotBlank()) {
                    lastNonBlankLineIndex = i
                    break
                }
            }

            if (lastNonBlankLineIndex != -1) {
                val potentialHashtagLine = contentLines[lastNonBlankLineIndex].trim()
                if (potentialHashtagLine.startsWith("#")) {
                    val subListToRemove = contentLines.subList(lastNonBlankLineIndex, contentLines.size)
                    subListToRemove.clear()
                }
            }
        }

        if (contentLines.all { it.isBlank() }) {
            return 0
        }

        val actualContent = contentLines.joinToString(separator = "\n")

        return actualContent.split(Regex("\\s+")).count { it.isNotBlank() }

    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

fun getRelativeTimeText(timestamp: Long): String {
    val now = Calendar.getInstance()
    val fileTime = Calendar.getInstance().apply { timeInMillis = timestamp }

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
