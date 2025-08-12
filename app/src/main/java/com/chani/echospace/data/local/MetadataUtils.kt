package com.chani.echospace.data.local

import java.text.SimpleDateFormat
import java.util.*
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class VirtualSegment(val key: String, val display: String)
typealias VirtualPath = List<VirtualSegment>

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

        // Remove hashtag line (last non-empty line that starts with #)
        val withoutHashtags = if (contentLines.isNotEmpty()) {
            val lastNonEmptyLine = getLastNonEmptyLine(contentLines.joinToString("\n"))

            if (lastNonEmptyLine != null && lastNonEmptyLine.startsWith("#") && !lastNonEmptyLine.startsWith("# ")) {
                // Remove the last non-empty line that contains hashtags
                val lastLineIndex = contentLines.indexOfLast { it.trim() == lastNonEmptyLine }
                contentLines.filterIndexed { index, _ -> index != lastLineIndex }
            } else {
                contentLines
            }
        } else {
            contentLines
        }

        // Join all content lines and clean markdown
        val cleanedContent = withoutHashtags
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
    @Suppress("RegExpRedundantEscape")
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

    // only check last line
    fun buildVirtualPathsFromContent(content: String): List<VirtualPath> {
        val lastLine = getLastNonEmptyLine(content) ?: return emptyList()
        if (!lastLine.startsWith("#")) return emptyList()
        return parseVirtualFolderRuns(lastLine)
    }

    // Returns the last non-empty line, trimmed of trailing newline characters but NOT inner spaces.
    private fun getLastNonEmptyLine(content: String): String? {
        if (content.isEmpty()) return null

        // Split into lines and work backwards to find the last non-empty line
        val lines = content.lines()

        for (i in lines.indices.reversed()) {
            val trimmedLine = lines[i].trim()
            if (trimmedLine.isNotEmpty()) {
                return trimmedLine
            }
        }

        return null
    }

    /**
     * Converts a string to proper camel case display format
     * Examples:
     * - "diary" -> "Diary"
     * - "DIARY" -> "Diary"
     * - "dream diary" -> "Dream Diary"
     * - "dREAm DiArY" -> "Dream Diary"
     * - "work-projects" -> "Work Projects"
     * - "personal_notes" -> "Personal Notes"
     */
    private fun formatDisplayName(input: String): String {
        if (input.isBlank()) return input

        // Split by non-alphanumeric characters to handle compound words while preserving numbers
        val words = input.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }

        return words.joinToString(" ") { word ->
            // Check if word is purely numeric
            if (word.all { it.isDigit() }) {
                word // Keep numbers as-is
            } else {
                // Apply camel case to text
                word.lowercase(Locale.getDefault()).replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            }
        }
    }

    // RECOMMENDED: Simpler approach - treat each # as segment boundary
    private fun parseVirtualFolderRuns(line: String): List<VirtualPath> {
        val result = mutableListOf<VirtualPath>()

        // Use regex to split by whitespace followed by hashtag, preserving the hashtag
        // This handles: "#diary dream #work#projects" -> ["#diary dream", "#work#projects"]
        val runs = line.trim().split(Regex("(?=\\s+#)")).filter { it.isNotBlank() }

        for (run in runs) {
            val trimmedRun = run.trim()
            if (!trimmedRun.startsWith("#")) continue

            // Split by # to get segments within this run, filter empty ones
            val segmentTexts = trimmedRun.split("#").filter { it.isNotBlank() }

            if (segmentTexts.isEmpty()) continue

            val segments = mutableListOf<VirtualSegment>()

            for (segmentText in segmentTexts) {
                val key = segmentText.lowercase(Locale.getDefault())
                val display = formatDisplayName(segmentText)
                segments.add(VirtualSegment(key = key, display = display))
            }

            if (segments.isNotEmpty()) {
                result.add(segments)
            }
        }

        return result
    }

    // Utility to pretty-print a virtual path for UI or debugging: "folder/sub/leaf" using display names.
    fun virtualPathDisplay(path: VirtualPath): String =
        path.joinToString(separator = "/") { it.display }

}
