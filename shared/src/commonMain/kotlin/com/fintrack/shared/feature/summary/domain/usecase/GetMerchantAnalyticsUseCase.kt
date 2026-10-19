package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.MerchantSummary
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository

class GetMerchantAnalyticsUseCase(
    private val summaryRepository: SummaryRepository
) {
    suspend operator fun invoke(
        accountId: String? = null,
        limit: Long = 10
    ): Result<List<MerchantSummary>> {
        return summaryRepository.getTopMerchants(accountId = accountId, limit = limit)
    }
}
