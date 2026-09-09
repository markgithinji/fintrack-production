package com.fintrack.shared.feature.auth.di

import com.fintrack.shared.feature.auth.data.remote.AuthApi
import com.fintrack.shared.feature.auth.data.repository.AuthRepositoryImpl
import com.fintrack.shared.feature.auth.domain.repository.AuthRepository
import com.fintrack.shared.feature.auth.domain.usecase.ChangePasswordValidationUseCase
import com.fintrack.shared.feature.auth.domain.usecase.LoginValidationUseCase
import com.fintrack.shared.feature.auth.domain.usecase.RegisterValidationUseCase
import com.fintrack.shared.feature.auth.ui.AuthViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    single { AuthApi(get()) }
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    singleOf(::RegisterValidationUseCase)
    singleOf(::LoginValidationUseCase)
    singleOf(::ChangePasswordValidationUseCase)
    viewModelOf(::AuthViewModel)
}
