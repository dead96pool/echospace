package com.chani.echospace.data.local

/**
 * Sealed class representing the result of file operations
 */
sealed class FileOperationResult {
    /**
     * Indicates successful file operation
     */
    data object Success : FileOperationResult()

    /**
     * Indicates failed file operation with error message
     */
    data class Error(val message: String) : FileOperationResult()
}
