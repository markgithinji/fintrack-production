package com.fintrack.shared.feature.transaction.di

import com.fintrack.shared.feature.transaction.data.TransactionApi
import com.fintrack.shared.feature.transaction.data.TransactionRepositoryImpl
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.ExportTransactionsUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.GetSpendingSummaryUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.SyncRecurringBillsUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.ValidateTransactionUseCase
import com.fintrack.shared.feature.transaction.ui.TransactionViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val transactionModule = module {
    singleOf(::TransactionApi)
    singleOf(::TransactionRepositoryImpl) { bind<TransactionRepository>() }

    singleOf(::ValidateTransactionUseCase)
    singleOf(::CreateTransactionUseCase)
    singleOf(::ExportTransactionsUseCase)
    singleOf(::SyncRecurringBillsUseCase)
    singleOf(::GetSpendingSummaryUseCase)

    viewModelOf(::TransactionViewModel)
}
