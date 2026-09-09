package com.fintrack.shared.feature.summary.di

import com.fintrack.shared.feature.summary.data.network.SummaryApi
import com.fintrack.shared.feature.summary.data.repository.SummaryRepositoryImpl
import com.fintrack.shared.feature.summary.domain.repository.SummaryRepository
import com.fintrack.shared.feature.summary.ui.StatisticsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val summaryModule = module {
    singleOf(::SummaryApi)
    singleOf(::SummaryRepositoryImpl) { bind<SummaryRepository>() }
    viewModelOf(::StatisticsViewModel)
}