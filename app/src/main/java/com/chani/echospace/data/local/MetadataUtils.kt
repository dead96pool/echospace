package com.chani.echospace.data.local

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object MetadataUtils {

    /**
     * Counts words in diary content, excluding the header line
     * Requirements:
     * - Don't include the header (e.g., "# filename 02:36 pm")
     * - Only count space-separated words
     * - Empty files should return 0
     */
    fun countWords(content: String): Int {
        if (content.isBlank()) return 0

        val lines = content.lines()

        // Exclude first line if it starts with # (header)
        val contentLines = if (lines.isNotEmpty() && lines[0].startsWith("#")) {
            lines.drop(1) // Remove the first line
        } else {
            lines
        }

        // Join all content lines and clean markdown
        val cleanedContent = contentLines
            .joinToString(" ") // Combine all lines into one string
            .let { removeMarkdownFormatting(it) } // Remove markdown symbols

        // Join all content lines and split by whitespace
        return cleanedContent
            .split(Regex("\\s+")) // Split by any whitespace (spaces, tabs, newlines)
            .filter { it.isNotBlank() } // Remove empty strings
            .size // Count the remaining words
    }

    /**
     * Removes markdown formatting symbols while preserving content
     */
    private fun removeMarkdownFormatting(text: String): String {
        var cleaned = text

        // Remove headers: # ## ### text → text
        cleaned = cleaned.replace(Regex("^#{1,6}\\s+"), "")

        // Remove list markers: - * + text → text
        cleaned = cleaned.replace(Regex("^[\\-\\*\\+]\\s+"), "")

        // Remove blockquote markers: > text → text
        cleaned = cleaned.replace(Regex("^>\\s+"), "")

        // Remove remaining markdown characters that might be standalone
        cleaned = cleaned.replace(Regex("[\\*_`~\\[\\]\\(\\)#>\\-\\+]"), " ")

        // Clean up multiple spaces
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        return cleaned
    }

    /**
     * Formats last modified timestamp according to requirements:
     * - Today's files: "02.47pm" format (12-hour time)
     * - Other files: "24.01.2025" format (dd.mm.yyyy)
     */
    fun formatLastModified(timestamp: Long): String {
        val date = Date(timestamp)
        val now = Date()

        // Convert to LocalDate for proper calendar-based comparison
        val dateLocal = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val todayLocal = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

        return when {
            // Today - same calendar date
            dateLocal.isEqual(todayLocal) -> {
                SimpleDateFormat("hh.mma", Locale.getDefault())
                    .format(date)
                    .lowercase(Locale.getDefault())
                    .replace("a.m.", "am")
                    .replace("p.m.", "pm")
            }

            // Yesterday - exactly 1 calendar day difference
            dateLocal.isEqual(todayLocal.minusDays(1)) -> {
                "yesterday"
            }

            // Multiple days ago - accurate calendar-based calculation
            else -> {
                val daysDiff = ChronoUnit.DAYS.between(dateLocal, todayLocal).toInt()
                "$daysDiff days ago"
            }
        }
    }

}
