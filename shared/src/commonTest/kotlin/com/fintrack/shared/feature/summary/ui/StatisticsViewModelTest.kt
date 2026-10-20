package com.fintrack.shared.feature.summary.ui

import com.fintrack.shared.feature.account.domain.repository.FakeAccountRepository
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.summary.domain.model.OverviewPeriod
import com.fintrack.shared.feature.summary.domain.usecase.GetCashflowRunwayUseCase
import com.fintrack.shared.feature.transaction.domain.repository.FakeTransactionRepository
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val accountRepository = FakeAccountRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val getCashflowRunwayUseCase = GetCashflowRunwayUseCase(accountRepository, transactionRepository)

    private lateinit var viewModel: StatisticsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val fakeSummaryRepo = object : com.fintrack.shared.feature.summary.domain.repository.SummaryRepository {
            override suspend fun getHighlightsSummary(accountId: String?, period: String?): Result<com.fintrack.shared.feature.summary.domain.model.StatisticsSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.StatisticsSummary("", false))
            override suspend fun getDistributionSummary(weekOrMonthCode: String, type: String?, start: String?, end: String?, accountId: String?): Result<com.fintrack.shared.feature.summary.domain.model.DistributionSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.DistributionSummary("", BigDecimal.ZERO, emptyList(), emptyList()))
            override suspend fun getAvailableWeeks(accountId: String?): Result<com.fintrack.shared.feature.summary.domain.model.AvailableWeeks> = Result.Success(com.fintrack.shared.feature.summary.domain.model.AvailableWeeks(emptyList()))
            override suspend fun getAvailableMonths(accountId: String?): Result<com.fintrack.shared.feature.summary.domain.model.AvailableMonths> = Result.Success(com.fintrack.shared.feature.summary.domain.model.AvailableMonths(emptyList()))
            override suspend fun getAvailableYears(accountId: String?): Result<com.fintrack.shared.feature.summary.domain.model.AvailableYears> = Result.Success(com.fintrack.shared.feature.summary.domain.model.AvailableYears(emptyList()))
            override suspend fun getOverviewSummary(accountId: String?, startDate: String?, endDate: String?): Result<com.fintrack.shared.feature.summary.domain.model.OverviewSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.OverviewSummary("", false, emptyList(), emptyList()))
            override suspend fun getCategoryComparisons(accountId: String?, period: String?): Result<com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.CategoryComparisonSummary("", false, emptyList()))
            override suspend fun getTransactionCounts(accountId: String, isIncome: Boolean?, categoryId: String?, start: String?, end: String?, hasTransactionCost: Boolean?): Result<com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary> = Result.Success(com.fintrack.shared.feature.summary.domain.model.TransactionCountSummary(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO))
            override suspend fun getProfileMetrics(): Result<com.fintrack.shared.feature.summary.domain.model.ProfileMetrics> = Result.Success(com.fintrack.shared.feature.summary.domain.model.ProfileMetrics("", "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
            override suspend fun getTopMerchants(accountId: String?, days: Int, limit: Long): Result<List<com.fintrack.shared.feature.summary.domain.model.MerchantSummary>> = Result.Success(emptyList())
        }

        viewModel = StatisticsViewModel(
            summaryRepository = fakeSummaryRepo,
            accountRepository = accountRepository,
            getCashflowRunwayUseCase = getCashflowRunwayUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `overview period initializes and updates independently`() = runTest {
        val testPeriod = Period.Month("2026-10")
        viewModel.onOverviewPeriodChanged(testPeriod)
        assertEquals(testPeriod, viewModel.selectedOverviewPeriod.value)
    }
}
