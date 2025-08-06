package com.chani.echospace.data.local

import org.junit.Test
import org.junit.Assert.*
import java.util.*
import java.util.concurrent.TimeUnit

class MetadataUtilsTest {

    @Test
    fun `formatLastModified shows correct format for today`() {
        val now = System.currentTimeMillis()
        val result = MetadataUtils.formatLastModified(now)

        // Should match time format pattern (e.g., "07.49pm")
        assertTrue("Today's file should show time format",
            result.matches(Regex("\\d{2}\\.\\d{2}[ap]m")))
    }

    @Test
    fun `formatLastModified shows yesterday correctly`() {
        val yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val result = MetadataUtils.formatLastModified(yesterday)

        assertEquals("Yesterday's file should show 'yesterday'", "yesterday", result)
    }

    @Test
    fun `formatLastModified shows multiple days ago correctly`() {
        val threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        val result = MetadataUtils.formatLastModified(threeDaysAgo)

        assertEquals("3 days ago should show correctly", "3 days ago", result)
    }

    @Test
    fun `formatLastModified handles edge cases correctly`() {
        val calendar = Calendar.getInstance()

        // Test yesterday at 23:59 (should be "yesterday")
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - 1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val yesterdayLate = calendar.timeInMillis

        assertEquals("Yesterday late should show 'yesterday'",
            "yesterday", MetadataUtils.formatLastModified(yesterdayLate))

        // Test today at 00:01 (should show time format)
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 1)
        val todayEarly = calendar.timeInMillis

        val todayResult = MetadataUtils.formatLastModified(todayEarly)
        assertTrue("Today early should show time format",
            todayResult.matches(Regex("\\d{2}\\.\\d{2}[ap]m")))
    }

    @Test
    fun `formatLastModified handles various day differences`() {
        val testCases = listOf(2, 5, 10, 30, 365)

        testCases.forEach { days ->
            val timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
            val result = MetadataUtils.formatLastModified(timestamp)

            assertEquals("$days days ago should show correctly",
                "$days days ago", result)
        }
    }
}
