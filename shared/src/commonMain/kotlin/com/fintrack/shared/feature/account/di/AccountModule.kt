package com.fintrack.shared.feature.account.di

import com.fintrack.shared.feature.account.data.remote.AccountsApi
import com.fintrack.shared.feature.account.data.repository.AccountRepositoryImpl
import com.fintrack.shared.feature.account.domain.repository.AccountRepository
import com.fintrack.shared.feature.account.domain.usecase.GetAccountsUseCase
import com.fintrack.shared.feature.account.ui.AccountsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountModule = module {
    singleOf(::AccountsApi)
    singleOf(::AccountRepositoryImpl) { bind<AccountRepository>() }
    factoryOf(::GetAccountsUseCase)
    viewModelOf(::AccountsViewModel)
}