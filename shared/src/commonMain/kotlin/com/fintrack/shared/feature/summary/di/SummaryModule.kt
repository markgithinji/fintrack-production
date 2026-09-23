package com.fintrack.shared.feature.summary.di

import com.fintrack.shared.feature.summary.data.repository.SummaryRepositoryOfflineImpl
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.summary.ui.StatisticsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val summaryModule = module {
    single<SummaryRepository> {
        SummaryRepositoryOfflineImpl(
            database = get(), 
            logger = get(),
            userRepository = get(),
            budgetRepository = get()
        )
    }

    viewModelOf(::StatisticsViewModel)
}
