package com.fintrack.shared.feature.transaction.domain.service

interface TransactionImporter {
    suspend fun importHistory(
        targetAccountId: String? = null,
        isPortfolioSeed: Boolean = false,
        onProgress: (Float) -> Unit = {}
    )
}
