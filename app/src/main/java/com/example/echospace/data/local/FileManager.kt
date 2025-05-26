// data/local/FileManager.kt
// Modify your existing FileManager class to fit this structure
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
        val sanitizedName = sanitizeFileName(fileName)
        val file = File(rootDir, "$sanitizedName.txt")

        return try {
            if (file.createNewFile()) {
                file
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun createFileWithCurrentDate(): File? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fileName = dateFormat.format(Date())
        return createFile(fileName)
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
}
