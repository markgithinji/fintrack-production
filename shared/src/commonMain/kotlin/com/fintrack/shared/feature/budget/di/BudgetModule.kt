package com.fintrack.shared.feature.budget.di

import com.fintrack.shared.feature.budget.data.repository.BudgetRepositoryImpl
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.budget.domain.usecase.BudgetValidationUseCase
import com.fintrack.shared.feature.budget.domain.usecase.CheckBudgetThresholdsUseCase
import com.fintrack.shared.feature.budget.ui.BudgetViewModel
import com.fintrack.shared.feature.transaction.domain.service.ReceiptScanner
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val budgetModule = module {
    single<BudgetRepository> { BudgetRepositoryImpl(database = get(), logger = get()) }
    singleOf(::BudgetValidationUseCase)
    singleOf(::CheckBudgetThresholdsUseCase)
    viewModel { 
        BudgetViewModel(
            budgetRepository = get(),
            validationUseCase = get(),
            localCategoryDataSource = get(),
            syncCategoriesUseCase = get(),
            receiptScanner = getOrNull<ReceiptScanner>()
        )
    }
}