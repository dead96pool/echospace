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

    // ================= VIRTUAL FOLDER PARSING TESTS =================

    @Test
    fun `buildVirtualPathsFromContent returns empty for content without hashtags`() {
        val testCases = listOf(
            "Regular content without hashtags",
            "Content with\nmultiple lines\nbut no hashtags",
            "",
            "   \n  \n  ",
            "Content ending with regular text"
        )

        testCases.forEach { content ->
            val result = MetadataUtils.buildVirtualPathsFromContent(content)
            assertTrue("Content '$content' should return empty paths", result.isEmpty())
            println("✅ No hashtags detected in: '$content'")
        }
    }

    @Test
    fun `buildVirtualPathsFromContent parses simple single hashtag`() {
        val testContent = """
            Some diary content here
            More content
            #diary
        """.trimIndent()

        val result = MetadataUtils.buildVirtualPathsFromContent(testContent)

        println("🔍 Testing: '$testContent'")
        println("📂 Last line detected: '#diary'")

        assertEquals("Should have 1 virtual path", 1, result.size)
        assertEquals("Path should have 1 segment", 1, result[0].size)
        assertEquals("Segment key should be lowercase", "diary", result[0][0].key)
        assertEquals("Segment display should be camel case", "Diary", result[0][0].display)

        println("✅ Virtual folder structure: /Diary/")
        println("   - Key: '${result[0][0].key}' | Display: '${result[0][0].display}'")
    }

    @Test
    fun `buildVirtualPathsFromContent parses multi-level hashtag hierarchy`() {
        val testContent = """
            Today I worked on my project
            Making good progress
            #work#projects#android
        """.trimIndent()

        val result = MetadataUtils.buildVirtualPathsFromContent(testContent)

        println("🔍 Testing: '$testContent'")
        println("📂 Last line detected: '#work#projects#android'")

        assertEquals("Should have 1 virtual path", 1, result.size)
        assertEquals("Path should have 3 segments", 3, result[0].size)

        val segments = result[0]
        assertEquals("First segment key", "work", segments[0].key)
        assertEquals("First segment display", "Work", segments[0].display)
        assertEquals("Second segment key", "projects", segments[1].key)
        assertEquals("Second segment display", "Projects", segments[1].display)
        assertEquals("Third segment key", "android", segments[2].key)
        assertEquals("Third segment display", "Android", segments[2].display)

        println("✅ Virtual folder structure: /Work/Projects/Android/")
        println("   - Level 1: '${segments[0].key}' → '${segments[0].display}'")
        println("   - Level 2: '${segments[1].key}' → '${segments[1].display}'")
        println("   - Level 3: '${segments[2].key}' → '${segments[2].display}'")
    }

    @Test
    fun `buildVirtualPathsFromContent handles multiple separate hashtag chains`() {
        val testContent = """
            Mixed content about work and personal stuff
            #work#urgent #personal#family
        """.trimIndent()

        val result = MetadataUtils.buildVirtualPathsFromContent(testContent)

        println("🔍 Testing: '$testContent'")
        println("📂 Last line detected: '#work#urgent #personal#family'")

        assertEquals("Should have 2 virtual paths", 2, result.size)

        // First chain: work#urgent
        assertEquals("First path should have 2 segments", 2, result[0].size)
        assertEquals("work key", "work", result[0][0].key)
        assertEquals("work display", "Work", result[0][0].display)
        assertEquals("urgent key", "urgent", result[0][1].key)
        assertEquals("urgent display", "Urgent", result[0][1].display)

        // Second chain: personal#family
        assertEquals("Second path should have 2 segments", 2, result[1].size)
        assertEquals("personal key", "personal", result[1][0].key)
        assertEquals("personal display", "Personal", result[1][0].display)
        assertEquals("family key", "family", result[1][1].key)
        assertEquals("family display", "Family", result[1][1].display)

        println("✅ Virtual folder structures:")
        println("   1. /Work/Urgent/")
        println("   2. /Personal/Family/")

        // Verify the file will appear in both locations
        println("📁 This file will appear in BOTH virtual folders!")
    }

    @Test
    fun `buildVirtualPathsFromContent handles camel case formatting correctly`() {
        val testCases = mapOf(
            "#diary" to "Diary",
            "#DIARY" to "Diary",
            "#dIARy" to "Diary",
            "#dream diary" to "Dream Diary",
            "#DREAM DIARY" to "Dream Diary",
            "#work-projects" to "Work Projects",
            "#personal_notes" to "Personal Notes",
            "#2025" to "2025",
            "#diary2025" to "Diary2025",
            "#my-dream_diary" to "My Dream Diary"
        )

        testCases.forEach { (hashtag, expectedDisplay) ->
            val content = "Some content\n$hashtag"
            val result = MetadataUtils.buildVirtualPathsFromContent(content)

            println("🔍 Testing hashtag: '$hashtag'")
            assertEquals("Should have 1 path", 1, result.size)
            assertEquals("Should have 1 segment", 1, result[0].size)
            assertEquals("Display should be formatted correctly", expectedDisplay, result[0][0].display)
            println("✅ '$hashtag' → Display: '$expectedDisplay' | Key: '${result[0][0].key}'")
        }
    }

    @Test
    fun `buildVirtualPathsFromContent handles mixed word hashtags`() {
        val testContent = """
            Today was productive
            #dream diary#2025#august work
        """.trimIndent()

        val result = MetadataUtils.buildVirtualPathsFromContent(testContent)

        println("🔍 Testing: '$testContent'")
        println("📂 Last line detected: '#dream diary#2025#august work'")

        assertEquals("Should have 1 virtual path", 1, result.size)
        assertEquals("Path should have 3 segments", 3, result[0].size)

        val segments = result[0]
        assertEquals("First segment display", "Dream Diary", segments[0].display)
        assertEquals("Second segment display", "2025", segments[1].display)
        assertEquals("Third segment display", "August Work", segments[2].display)

        println("✅ Virtual folder structure: /Dream Diary/2025/August Work/")
        segments.forEachIndexed { index, segment ->
            println("   - Level ${index + 1}: '${segment.key}' → '${segment.display}'")
        }
    }

    @Test
    fun `buildVirtualPathsFromContent ignores hashtags not in last line`() {
        val testContent = """
            # Header with hash
            Content with #hashtag in middle
            More content here
            final line without hashtag
        """.trimIndent()

        val result = MetadataUtils.buildVirtualPathsFromContent(testContent)

        println("🔍 Testing: '$testContent'")
        println("📂 Last line detected: 'final line without hashtag'")

        assertTrue("Should return empty as hashtag not in last line", result.isEmpty())
        println("✅ Correctly ignored hashtags not in last line")
    }

    @Test
    fun `buildVirtualPathsFromContent handles complex real-world scenarios`() {
        val complexScenarios = listOf(
            // Scenario 1: Daily diary with date hierarchy
            Triple(
                "Daily reflection\n#diary#2025#august#reflection",
                "Daily diary entry with date hierarchy",
                listOf(listOf("diary" to "Diary", "2025" to "2025", "august" to "August", "reflection" to "Reflection"))
            ),

            // Scenario 2: Work project with multiple tags
            Triple(
                "Sprint planning meeting\n#work#sprint-planning #meetings#weekly",
                "Work with multiple tag chains",
                listOf(
                    listOf("work" to "Work", "sprint-planning" to "Sprint Planning"),
                    listOf("meetings" to "Meetings", "weekly" to "Weekly")
                )
            ),

            // Scenario 3: Mixed personal and work
            Triple(
                "Balanced day\n#work#coding #personal#family#vacation-planning",
                "Mixed personal and work content",
                listOf(
                    listOf("work" to "Work", "coding" to "Coding"),
                    listOf("personal" to "Personal", "family" to "Family", "vacation-planning" to "Vacation Planning")
                )
            )
        )

        complexScenarios.forEach { (content, description, expectedStructure) ->
            println("\n🔍 Testing: $description")
            println("📄 Content: '$content'")

            val result = MetadataUtils.buildVirtualPathsFromContent(content)

            assertEquals("Should have ${expectedStructure.size} paths", expectedStructure.size, result.size)

            expectedStructure.forEachIndexed { pathIndex, expectedPath ->
                assertEquals("Path $pathIndex should have ${expectedPath.size} segments", expectedPath.size, result[pathIndex].size)

                val pathDisplay = expectedPath.map { it.second }.joinToString("/")
                println("✅ Virtual folder structure ${pathIndex + 1}: /$pathDisplay/")

                expectedPath.forEachIndexed { segIndex, (expectedKey, expectedDisplay) ->
                    assertEquals("Path $pathIndex segment $segIndex key", expectedKey, result[pathIndex][segIndex].key)
                    assertEquals("Path $pathIndex segment $segIndex display", expectedDisplay, result[pathIndex][segIndex].display)
                    println("   - Level ${segIndex + 1}: '${result[pathIndex][segIndex].key}' → '${result[pathIndex][segIndex].display}'")
                }
            }

            if (expectedStructure.size > 1) {
                println("📁 This file will appear in ${expectedStructure.size} different virtual folder locations!")
            }
        }
    }

    @Test
    fun `buildVirtualPathsFromContent handles edge cases and malformed input`() {
        val edgeCases = mapOf(
            "Content\n#" to "Single hash symbol",
            "Content\n##" to "Double hash symbols",
            "Content\n# " to "Hash with space",
            "Content\n#diary#" to "Trailing hash",
            "Content\n##diary" to "Double hash prefix",
            "Content\n#diary##work" to "Mixed hash counts",
            "Content\n#diary #" to "Mixed valid and invalid",
            "Content\n   #diary   " to "Hashtag with surrounding whitespace"
        )

        edgeCases.forEach { (content, description) ->
            println("\n🔍 Testing edge case: $description")
            println("📄 Content: '$content'")

            val result = MetadataUtils.buildVirtualPathsFromContent(content)

            println("📂 Parsed paths: ${result.size}")
            result.forEachIndexed { pathIndex, path ->
                val pathDisplay = path.map { it.display }.joinToString("/")
                println("   ${pathIndex + 1}. /$pathDisplay/")
                path.forEach { segment ->
                    println("      - '${segment.key}' → '${segment.display}'")
                }
            }

            // Basic validation - results should never contain empty segments
            result.forEach { path ->
                path.forEach { segment ->
                    assertFalse("Segment key should not be empty", segment.key.isEmpty())
                    assertFalse("Segment display should not be empty", segment.display.isEmpty())
                }
            }
        }
    }

    @Test
    fun `virtualPathDisplay utility formats paths correctly`() {
        val testPath = listOf(
            VirtualSegment(key = "work", display = "Work"),
            VirtualSegment(key = "projects", display = "Projects"),
            VirtualSegment(key = "android", display = "Android")
        )

        val displayPath = MetadataUtils.virtualPathDisplay(testPath)
        assertEquals("Should join with forward slashes", "Work/Projects/Android", displayPath)

        println("🔍 Testing virtualPathDisplay utility")
        println("✅ Path display: '$displayPath'")
    }

    // ================= INTEGRATION TEST =================

    @Test
    fun `complete virtual folder workflow simulation`() {
        println("\n" + "=".repeat(60))
        println("🚀 COMPLETE VIRTUAL FOLDER WORKFLOW SIMULATION")
        println("=".repeat(60))

        val simulatedFiles = listOf(
            "Daily standup meeting notes\n#work#meetings#daily",
            "Worked on authentication feature\n#work#coding#android#authentication",
            "Family dinner planning\n#personal#family#planning",
            "Weekend hiking trip\n#personal#hobbies#hiking #travel#weekend",
            "Sprint retrospective thoughts\n#work#meetings#retrospective #personal#reflection",
            "Grocery shopping list\n#personal#household",
            "Debug session for login bug\n#work#debugging#authentication#bugs"
        )

        println("📁 Simulating ${simulatedFiles.size} diary files...")
        println()

        val allVirtualPaths = mutableListOf<List<VirtualPath>>()
        val virtualFolderStructure = mutableMapOf<String, MutableList<String>>()

        simulatedFiles.forEachIndexed { index, fileContent ->
            println("📄 File ${index + 1}:")
            val lastLine = fileContent.lines().last()
            println("   Content: '${fileContent.lines().first()}...'")
            println("   Last line: '$lastLine'")

            val virtualPaths = MetadataUtils.buildVirtualPathsFromContent(fileContent)
            allVirtualPaths.add(virtualPaths)

            if (virtualPaths.isEmpty()) {
                println("   📂 No virtual folders (file goes to root)")
                virtualFolderStructure.getOrPut("ROOT") { mutableListOf() }.add("File ${index + 1}")
            } else {
                virtualPaths.forEach { path ->
                    val folderPath = "/" + path.map { it.display }.joinToString("/") + "/"
                    println("   📂 Virtual folder: $folderPath")
                    virtualFolderStructure.getOrPut(folderPath) { mutableListOf() }.add("File ${index + 1}")
                }
                if (virtualPaths.size > 1) {
                    println("   🔗 File appears in ${virtualPaths.size} locations!")
                }
            }
            println()
        }

        println("📊 VIRTUAL FOLDER STRUCTURE SUMMARY:")
        println("=".repeat(40))

        virtualFolderStructure.toSortedMap().forEach { (folderPath, files) ->
            println("📁 $folderPath")
            files.forEach { file ->
                println("   └── $file")
            }
            println("   (${files.size} files)")
            println()
        }

        println("🎯 STATISTICS:")
        println("   • Total files: ${simulatedFiles.size}")
        println("   • Unique virtual folders: ${virtualFolderStructure.size}")
        println("   • Files with multiple locations: ${allVirtualPaths.count { it.size > 1 }}")
        println("   • Max folder depth: ${virtualFolderStructure.keys.maxOfOrNull { it.count { c -> c == '/' } - 1 } ?: 0}")

        // Verify the virtual folder system is working correctly
        assertTrue("Should have virtual folders", virtualFolderStructure.isNotEmpty())
        assertTrue("Should have work-related folders",
            virtualFolderStructure.keys.any { it.contains("Work") })
        assertTrue("Should have personal folders",
            virtualFolderStructure.keys.any { it.contains("Personal") })
    }

}
