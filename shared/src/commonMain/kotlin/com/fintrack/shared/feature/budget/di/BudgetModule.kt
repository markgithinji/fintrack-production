package com.fintrack.shared.feature.budget.di

import com.fintrack.shared.feature.budget.data.repository.BudgetRepositoryOfflineImpl
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.budget.domain.usecase.BudgetValidationUseCase
import com.fintrack.shared.feature.budget.domain.usecase.CheckBudgetThresholdsUseCase
import com.fintrack.shared.feature.budget.ui.BudgetViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val budgetModule = module {
    single<BudgetRepository> { BudgetRepositoryOfflineImpl(database = get(), logger = get()) }
    singleOf(::BudgetValidationUseCase)
    singleOf(::CheckBudgetThresholdsUseCase)
    viewModelOf(::BudgetViewModel)
}