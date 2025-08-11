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

    // only check last line
    fun buildVirtualPathsFromContent(content: String): List<VirtualPath> {
        val lastLine = getLastNonEmptyLine(content) ?: return emptyList()
        if (!lastLine.startsWith("#")) return emptyList()
        return parseVirtualFolderRuns(lastLine)
    }

    // Returns the last non-empty line, trimmed of trailing newline characters but NOT inner spaces.
    private fun getLastNonEmptyLine(content: String): String? {
        if (content.isEmpty()) return null
        var i = content.length - 1

        // Skip trailing newlines
        while (i >= 0 && (content[i] == '\n' || content[i] == '\r')) i--
        if (i < 0) return null

        // Find this line bounds
        val end = i
        var start = i
        while (start >= 0 && content[start] != '\n' && content[start] != '\r') start--
        val line = content.substring(start + 1, end + 1)
        val trimmed = line.trim()

        if (trimmed.isNotEmpty()) return trimmed

        // If blank, recurse on previous portion
        val prev = content.substring(0, (start).coerceAtLeast(0))
        return getLastNonEmptyLine(prev)
    }

    // Parse multiple tag runs on the same line.
    // Example: "#a#b   #c" -> [ ["a","b"], ["c"] ]
    private fun parseVirtualFolderRuns(line: String): List<VirtualPath> {
        val result = mutableListOf<VirtualPath>()
        var idx = 0
        val len = line.length

        fun isWhitespace(c: Char) = c == ' ' || c == '\t'

        while (idx < len) {
            // Skip whitespace between runs
            while (idx < len && isWhitespace(line[idx])) idx++
            if (idx >= len) break

            if (line[idx] != '#') {
                // Ignore non-# tokens between runs
                while (idx < len && !isWhitespace(line[idx]) && line[idx] != '#') idx++
                continue
            }

            // Start of a run (a path)
            val segments = mutableListOf<VirtualSegment>()

            while (idx < len && line[idx] == '#') {
                // Count consecutive '#'
                var hashStart = idx
                while (idx < len && line[idx] == '#') idx++
                val hashCount = idx - hashStart

                val literalPrefixCount = hashCount - 1
                val literalPrefix = if (literalPrefixCount > 0) "#".repeat(literalPrefixCount) else ""

                // Collect segment body until next '#' or whitespace
                val segStart = idx
                while (idx < len && line[idx] != '#' && !isWhitespace(line[idx])) idx++
                val body = line.substring(segStart, idx)

                if (body.isNotEmpty()) {
                    val raw = literalPrefix + body
                    val key = raw.lowercase(Locale.getDefault())
                    segments.add(VirtualSegment(key = key, display = raw))
                }

                // If next is whitespace, this run ends
                if (idx < len && isWhitespace(line[idx])) break
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
