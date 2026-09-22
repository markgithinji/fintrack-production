package com.fintrack.shared.feature.transaction.service

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.service.ReceiptScanner
import com.fintrack.shared.feature.transaction.domain.service.ScannedReceiptData

class IosReceiptScanner : ReceiptScanner {
    override suspend fun scanReceipt(imageBytes: ByteArray): Result<ScannedReceiptData> {
        // Basic stub for iOS. A proper implementation would use Vision framework (VNRecognizeTextRequest).
        return Result.Error(Exception("Receipt scanning is not yet supported on iOS."))
    }
}
