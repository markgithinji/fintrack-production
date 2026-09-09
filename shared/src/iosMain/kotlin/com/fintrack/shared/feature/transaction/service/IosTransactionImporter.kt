package com.fintrack.shared.feature.transaction.service

import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter

class IosTransactionImporter : TransactionImporter {
    override suspend fun importHistory(
        targetAccountId: String?,
        isPortfolioSeed: Boolean,
        onProgress: (Float) -> Unit
    ) {
        onProgress(1.0f)
    }
}
