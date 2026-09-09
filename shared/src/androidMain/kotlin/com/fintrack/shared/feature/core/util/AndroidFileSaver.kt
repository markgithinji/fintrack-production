package com.fintrack.shared.feature.core.util

import android.content.Context
import android.os.Environment
import java.io.File

class AndroidFileSaver(private val context: Context) : FileSaver {
    override suspend fun saveFile(fileName: String, content: String): String? {
        return try {
            // Saving to public Downloads folder if possible, otherwise app-specific downloads
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                File(downloadsDir, fileName)
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            }
            
            file.writeText(content)
            file.absolutePath
        } catch (e: Exception) {
            // Fallback to internal storage if external fails
            try {
                val file = File(context.filesDir, fileName)
                file.writeText(content)
                file.absolutePath
            } catch (inner: Exception) {
                null
            }
        }
    }

    override suspend fun saveFileBytes(fileName: String, bytes: ByteArray): String? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                File(downloadsDir, fileName)
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            }
            
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            try {
                val file = File(context.filesDir, fileName)
                file.writeBytes(bytes)
                file.absolutePath
            } catch (inner: Exception) {
                null
            }
        }
    }
}
