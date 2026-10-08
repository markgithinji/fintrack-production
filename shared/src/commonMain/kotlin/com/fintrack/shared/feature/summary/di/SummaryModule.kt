package com.fintrack.shared.feature.summary.di

import com.fintrack.shared.feature.summary.data.repository.SummaryRepositoryImpl
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
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

    viewModelOf(::StatisticsViewModel)
    viewModelOf(::CustomAnalysisViewModel)
}
