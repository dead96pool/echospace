// data/local/FileManager.kt

package com.example.echospace.data.local

import android.content.Context
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManager(private val context: Context) {

    val rootDir: File by lazy {
        File(context.filesDir, "EchoSpaceFiles").apply {
            if (!exists()) {
                mkdir()
            }
        }
    }

    fun getAllFiles(): List<File> {
        return rootDir.listFiles()
            ?.filter { it.isFile && it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun createFile(fileName: String): File? {
        return try {
            val sanitizedName = sanitizeFileName(fileName)
            val file = File(rootDir, "$sanitizedName.txt")

            if (file.createNewFile()) {
                // Add filename and time as first line of editable content
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                                    .uppercase()
                                    .replace(".", "")
                val firstLine = "# $sanitizedName  $currentTime\n\n"

                file.writeText(firstLine)
                file
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun createFileWithCurrentDate(): File? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fileName = dateFormat.format(Date())
            val file = File(rootDir, "$fileName.txt")

            if (file.createNewFile()) {
                // Add date and time as first line of editable content
                val displayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val currentDate = Date()
                val displayDate = displayDateFormat.format(currentDate)
                val displayTime = timeFormat.format(currentDate)
                                    .uppercase()
                                    .replace(".", "")
                val firstLine = "# $displayDate  $displayTime\n\n"

                file.writeText(firstLine)
                file
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun readFileContent(file: File): String {
        return try {
            file.readText()
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    fun writeFileContent(file: File, content: String): Boolean {
        return try {
            file.writeText(content)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        // Remove invalid file name characters
        return fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    fun renameFile(file: File, newName: String): Boolean {
        val sanitizedName = sanitizeFileName(newName)
        val newFile = File(file.parentFile, "$sanitizedName.txt")

        return try {
            if (newFile.exists()) {
                return false // Don't overwrite existing files
            }
            file.renameTo(newFile)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun fileExists(fileName: String): Boolean {
        val sanitizedName = sanitizeFileName(fileName)
        val file = File(rootDir, "$sanitizedName.txt")
        return file.exists()
    }
}
