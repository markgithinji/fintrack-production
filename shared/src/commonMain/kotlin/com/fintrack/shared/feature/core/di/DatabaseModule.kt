package com.fintrack.shared.feature.core.di

import com.fintrack.shared.db.*
import org.koin.dsl.module

val databaseModule = module {
    single {
        val driverFactory: DriverFactory = get()
        val database = FintrackDatabase(
            driver = driverFactory.createDriver(),
            AccountEntityAdapter = AccountEntity.Adapter(
                createdAtAdapter = instantAdapter,
                lastSyncedAtAdapter = instantAdapter
            ),
            CategoryEntityAdapter = CategoryEntity.Adapter(
                createdAtAdapter = instantAdapter
            ),
            TransactionEntityAdapter = TransactionEntity.Adapter(
                dateTimeAdapter = instantAdapter,
                createdAtAdapter = instantAdapter,
                updatedAtAdapter = instantAdapter
            ),
            BudgetEntityAdapter = BudgetEntity.Adapter(
                createdAtAdapter = instantAdapter
            )
        )
        DatabaseSeeder(database).seedIfEmpty()
        database
    }
}
