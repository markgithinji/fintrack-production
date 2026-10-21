package com.fintrack.shared.feature.summary.domain.usecase

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.AvailableMonths
import com.fintrack.shared.feature.summary.domain.model.AvailableWeeks
import com.fintrack.shared.feature.summary.domain.model.AvailableYears
import com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary
import com.fintrack.shared.feature.summary.domain.model.DistributionSummary
import com.fintrack.shared.feature.summary.domain.model.Highlights
import com.fintrack.shared.feature.summary.domain.model.MerchantSummary
import com.fintrack.shared.feature.summary.domain.model.OverviewSummary
import com.fintrack.shared.feature.summary.domain.model.ProfileMetrics
import com.fintrack.shared.feature.summary.domain.model.StatisticsSummary
import com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetMerchantAnalyticsUseCaseTest {

    @Test
    fun `fetches top merchants successfully`() = runTest {
        val fakeRepo = object : SummaryRepository {
            override suspend fun getHighlightsSummary(accountId: String?, period: String?): Result<StatisticsSummary> =
                Result.Success(StatisticsSummary(period = "", isCurrent = false))

            override suspend fun getDistributionSummary(weekOrMonthCode: String, type: String?, start: String?, end: String?, accountId: String?): Result<DistributionSummary> =
                Result.Success(DistributionSummary("", BigDecimal.ZERO, emptyList(), emptyList()))

            override suspend fun getAvailableWeeks(accountId: String?): Result<AvailableWeeks> = Result.Success(AvailableWeeks(emptyList()))
            override suspend fun getAvailableMonths(accountId: String?): Result<AvailableMonths> = Result.Success(AvailableMonths(emptyList()))
            override suspend fun getAvailableYears(accountId: String?): Result<AvailableYears> = Result.Success(AvailableYears(emptyList()))
            override suspend fun getOverviewSummary(accountId: String?, startDate: String?, endDate: String?): Result<OverviewSummary> = Result.Success(OverviewSummary("", false, emptyList(), emptyList()))
            override suspend fun getCategoryComparisons(accountId: String?, period: String?): Result<CategoryComparisonSummary> = Result.Success(CategoryComparisonSummary("", false, emptyList()))
            override suspend fun getTransactionCounts(accountId: String, isIncome: Boolean?, categoryId: String?, start: String?, end: String?, hasTransactionCost: Boolean?): Result<TransactionCountSummary> = Result.Success(TransactionCountSummary(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO))
            override suspend fun getProfileMetrics(): Result<ProfileMetrics> = Result.Success(ProfileMetrics("", "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))

            override suspend fun getTopMerchants(accountId: String?, days: Int, limit: Long, isIncome: Boolean): Result<List<MerchantSummary>> {
                return Result.Success(
                    listOf(
                        MerchantSummary(
                            rank = 1,
                            merchantName = "Naivas Supermarket",
                            totalSpent = BigDecimal.fromInt(42000),
                            visitCount = 14,
                            averageTransactionCost = BigDecimal.fromInt(3000),
                            firstSeenDate = "2026-09-01",
                            lastSeenDate = "2026-10-18"
                        )
                    )
                )
            }
        }

        val useCase = GetMerchantAnalyticsUseCase(fakeRepo)
        val result = useCase()

        assertTrue(result is Result.Success)
        val merchants = (result as Result.Success).data
        assertEquals(1, merchants.size)
        assertEquals("Naivas Supermarket", merchants[0].merchantName)
        assertEquals(14, merchants[0].visitCount)
    }
}
