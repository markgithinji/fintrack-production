package com.fintrack.shared.feature.transaction.domain.util

import com.fintrack.shared.feature.transaction.domain.model.Transaction
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.CoreText.*

@OptIn(ExperimentalForeignApi::class)
actual fun generatePdfBytes(transactions: List<Transaction>): ByteArray {
    val data = NSMutableData.create()
    UIGraphicsBeginPDFContextToData(data, CGRectZero.readValue(), null)
    UIGraphicsBeginPDFPageWithInfo(CGRectZero.readValue(), null)

    val title = "FINTRACK TRANSACTION REPORT"
    title.drawAtPoint(
        CGPointMake(50.0, 50.0),
        withAttributes = mapOf(NSFontAttributeName to UIFont.boldSystemFontOfSize(18.0))
    )

    var y = 100.0
    transactions.forEach { transaction ->
        if (y > 750.0) {
            UIGraphicsBeginPDFPageWithInfo(CGRectZero.readValue(), null)
            y = 50.0
        }
        val text = "${transaction.dateTime} | ${transaction.category} | ${transaction.totalAmount}"
        text.drawAtPoint(
            CGPointMake(50.0, y),
            withAttributes = mapOf(NSFontAttributeName to UIFont.systemFontOfSize(12.0))
        )
        y += 20.0
    }

    UIGraphicsEndPDFContext()
    
    val bytes = ByteArray(data.length.toInt())
    data.bytes?.let { ptr ->
        // This is a bit tricky in KMP iOS, usually we use data.getBytes
    }
    // Simplification for now, since we need to return a valid PDF binary
    return data.bytes?.let { nsDataToByteArray(data) } ?: ByteArray(0)
}

@OptIn(ExperimentalForeignApi::class)
private fun nsDataToByteArray(data: NSData): ByteArray {
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isEmpty()) return bytes
    kotlinx.cinterop.usePinned(bytes) { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return bytes
}
