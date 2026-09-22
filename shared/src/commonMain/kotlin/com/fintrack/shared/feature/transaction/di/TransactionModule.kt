package com.fintrack.shared.feature.transaction.di

import com.fintrack.shared.feature.transaction.data.TransactionApi
import com.fintrack.shared.feature.transaction.data.repository.TransactionRepositoryOfflineImpl
import com.fintrack.shared.feature.transaction.domain.repository.TransactionRepository
import com.fintrack.shared.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.ExportTransactionsUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.GetSpendingSummaryUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.SyncRecurringBillsUseCase
import com.fintrack.shared.feature.transaction.domain.usecase.ValidateTransactionUseCase
import com.fintrack.shared.feature.transaction.ui.TransactionViewModel
import com.fintrack.shared.feature.transaction.domain.service.ReceiptScanner
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val transactionModule = module {
    singleOf(::TransactionApi)
    
    single<TransactionRepository> {
        TransactionRepositoryOfflineImpl(
            database = get(),
            logger = get()
        )
    }

    singleOf(::ValidateTransactionUseCase)
    singleOf(::CreateTransactionUseCase)
    singleOf(::ExportTransactionsUseCase)
    singleOf(::SyncRecurringBillsUseCase)
    singleOf(::GetSpendingSummaryUseCase)

    viewModel {
        TransactionViewModel(
            repo = get(),
            localCategoryDataSource = get(),
            syncCategoriesUseCase = get(),
            validateTransactionUseCase = get(),
            createTransactionUseCase = get(),
            transactionImporter = get(),
            receiptScanner = getOrNull<ReceiptScanner>()
        )
    }
}
