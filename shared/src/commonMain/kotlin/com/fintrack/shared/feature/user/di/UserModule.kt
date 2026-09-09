package com.fintrack.shared.feature.user.di

import com.fintrack.shared.feature.user.ui.ProfileViewModel
import com.fintrack.shared.feature.user.data.UserApi
import com.fintrack.shared.feature.user.data.UserRepositoryImpl
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.fintrack.shared.feature.user.domain.usecase.DeleteAccountUseCase
import com.fintrack.shared.feature.user.domain.usecase.ProfileValidationUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val userModule = module {
    singleOf(::UserApi)
    singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
    singleOf(::DeleteAccountUseCase)
    singleOf(::ProfileValidationUseCase)
    viewModelOf(::ProfileViewModel)
}
