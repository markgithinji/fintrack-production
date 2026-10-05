package com.fintrack.shared.feature.core.util

class FakeFileSaver : FileSaver {
    val savedFiles = mutableMapOf<String, String>()
    val savedFileBytes = mutableMapOf<String, ByteArray>()

    override suspend fun saveFile(fileName: String, content: String): String? {
        savedFiles[fileName] = content
        return "/path/to/$fileName"
    }

    override suspend fun saveFileBytes(fileName: String, bytes: ByteArray): String? {
        savedFileBytes[fileName] = bytes
        return "/path/to/$fileName"
    }
}
