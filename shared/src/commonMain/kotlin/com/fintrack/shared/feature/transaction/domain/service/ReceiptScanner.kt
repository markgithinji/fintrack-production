package com.fintrack.shared.feature.transaction.domain.service

import com.fintrack.shared.feature.core.util.Result
import kotlinx.datetime.LocalDate

data class ScannedReceiptData(
    val amount: Double?,
    val date: LocalDate?,
    val merchantName: String?,
    val isMpesa: Boolean,
    val suggestedCategoryName: String?,
    val rawText: String
)

interface ReceiptScanner {
    suspend fun scanReceipt(imageBytes: ByteArray): Result<ScannedReceiptData>
}
