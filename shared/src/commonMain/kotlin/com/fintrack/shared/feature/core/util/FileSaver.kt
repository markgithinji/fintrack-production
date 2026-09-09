package com.fintrack.shared.feature.core.util

interface FileSaver {
    suspend fun saveFile(fileName: String, content: String): String?
    suspend fun saveFileBytes(fileName: String, bytes: ByteArray): String?
}
