package com.fintrack.shared.feature.transaction.domain.service

class FakeTransactionImporter : TransactionImporter {
    override suspend fun importHistory(targetAccountId: String?, isPortfolioSeed: Boolean, onProgress: (Float) -> Unit) {}
}
