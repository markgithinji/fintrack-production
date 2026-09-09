package com.fintrack.shared.feature.core.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

class IosFileSaver : FileSaver {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun saveFile(fileName: String, content: String): String? {
        return try {
            val fileManager = NSFileManager.defaultManager
            val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            val documentDirectory = urls.first() as NSURL
            val fileURL = documentDirectory.URLByAppendingPathComponent(fileName)
            
            val nsString = NSString.create(string = content)
            nsString.writeToURL(fileURL!!, true, NSUTF8StringEncoding, null)
            fileURL.path
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun saveFileBytes(fileName: String, bytes: ByteArray): String? {
        return try {
            val fileManager = NSFileManager.defaultManager
            val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            val documentDirectory = urls.first() as NSURL
            val fileURL = documentDirectory.URLByAppendingPathComponent(fileName)

            val data = bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            data.writeToURL(fileURL!!, true)
            fileURL.path
        } catch (e: Exception) {
            null
        }
    }
}
