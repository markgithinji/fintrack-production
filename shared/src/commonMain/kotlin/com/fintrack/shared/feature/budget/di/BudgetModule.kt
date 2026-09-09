package com.fintrack.shared.feature.budget.di

import com.fintrack.shared.feature.budget.data.remote.BudgetApi
import com.fintrack.shared.feature.budget.data.repository.BudgetRepositoryImpl
import com.fintrack.shared.feature.budget.domain.repository.BudgetRepository
import com.fintrack.shared.feature.budget.domain.usecase.BudgetValidationUseCase
import com.fintrack.shared.feature.budget.domain.usecase.CheckBudgetThresholdsUseCase
import com.fintrack.shared.feature.budget.ui.BudgetViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val budgetModule = module {
    singleOf(::BudgetApi)
    singleOf(::BudgetRepositoryImpl) { bind<BudgetRepository>() }
    singleOf(::BudgetValidationUseCase)
    singleOf(::CheckBudgetThresholdsUseCase)
    viewModelOf(::BudgetViewModel)
}