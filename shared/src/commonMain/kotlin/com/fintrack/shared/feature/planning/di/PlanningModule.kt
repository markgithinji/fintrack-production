package com.fintrack.shared.feature.planning.di

import com.fintrack.shared.feature.budget.data.repository.BudgetRepositoryImpl
import com.fintrack.shared.feature.budget.data.repository.SavingsGoalRepositoryImpl
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.budget.domain.repository.SavingsGoalRepository
import com.fintrack.shared.feature.budget.domain.usecase.BudgetValidationUseCase
import com.fintrack.shared.feature.budget.domain.usecase.CheckBudgetThresholdsUseCase
import com.fintrack.shared.feature.budget.domain.usecase.GetSavingsGoalsUseCase
import com.fintrack.shared.feature.planning.data.repository.DebtRepositoryImpl
import com.fintrack.shared.feature.planning.domain.repository.DebtRepository
import com.fintrack.shared.feature.planning.domain.usecase.CalculateDebtPayoffPlanUseCase
import com.fintrack.shared.feature.planning.ui.PlanningViewModel
import com.fintrack.shared.feature.transaction.domain.service.ReceiptScanner
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val planningModule = module {
    single<BudgetRepository> { BudgetRepositoryImpl(database = get(), logger = get()) }
    single<SavingsGoalRepository> { SavingsGoalRepositoryImpl(database = get()) }
    single<DebtRepository> { DebtRepositoryImpl(database = get()) }
    singleOf(::BudgetValidationUseCase)
    singleOf(::CheckBudgetThresholdsUseCase)
    singleOf(::GetSavingsGoalsUseCase)
    singleOf(::CalculateDebtPayoffPlanUseCase)
    viewModel { 
        PlanningViewModel(
            budgetRepository = get(),
            savingsGoalRepository = get(),
            getSavingsGoalsUseCase = get(),
            validationUseCase = get(),
            localCategoryDataSource = get(),
            syncCategoriesUseCase = get(),
            debtRepository = get(),
            calculateDebtPayoffPlanUseCase = get(),
            receiptScanner = getOrNull<ReceiptScanner>()
        )
    }
}
