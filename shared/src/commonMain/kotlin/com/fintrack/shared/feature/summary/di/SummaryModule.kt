package com.fintrack.shared.feature.summary.di

import com.fintrack.shared.feature.summary.data.repository.SummaryRepositoryImpl
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.summary.domain.usecase.DetectSpendSpikesUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetBudgetOptimizerUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetCashflowRunwayUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetEmergencyFundScoreUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetMerchantAnalyticsUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetNetWorthSummaryUseCase
import com.fintrack.shared.feature.summary.domain.usecase.GetPassiveIncomeCoverageUseCase
import com.fintrack.shared.feature.summary.domain.usecase.SimulateAffordabilityUseCase
import com.fintrack.shared.feature.summary.ui.StatisticsViewModel
import com.fintrack.shared.feature.summary.ui.custom.CustomAnalysisViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val summaryModule = module {
    single<SummaryRepository> {
        SummaryRepositoryImpl(
            database = get(), 
            logger = get(),
            userRepository = get(),
            budgetRepository = get()
        )
    }

    singleOf(::GetCashflowRunwayUseCase)
    single {
        GetNetWorthSummaryUseCase(
            accountRepository = get(),
            transactionRepository = get(),
            savingsGoalRepository = getOrNull(),
            debtRepository = getOrNull()
        )
    }
    singleOf(::DetectSpendSpikesUseCase)
    singleOf(::GetEmergencyFundScoreUseCase)
    singleOf(::GetPassiveIncomeCoverageUseCase)
    single {
        SimulateAffordabilityUseCase(
            getCashflowRunwayUseCase = get(),
            getSavingsGoalsUseCase = getOrNull(),
            assetRepository = getOrNull(),
            debtRepository = getOrNull()
        )
    }
    singleOf(::GetMerchantAnalyticsUseCase)
    single {
        GetBudgetOptimizerUseCase(
            transactionRepository = get(),
            savingsGoalRepository = getOrNull()
        )
    }

    viewModelOf(::StatisticsViewModel)
    viewModelOf(::CustomAnalysisViewModel)
}
