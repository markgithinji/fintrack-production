package com.fintrack.shared.feature.user.di

import com.fintrack.shared.feature.user.data.UserRepositoryImpl
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import com.fintrack.shared.feature.user.domain.usecase.DeleteAccountUseCase
import com.fintrack.shared.feature.user.domain.usecase.ProfileValidationUseCase
import com.fintrack.shared.feature.user.ui.ProfileViewModel
import com.fintrack.shared.feature.user.ui.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val userModule = module {
    single<UserRepository> { UserRepositoryImpl(settingsDataSource = get()) }
    singleOf(::DeleteAccountUseCase)
    singleOf(::ProfileValidationUseCase)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::OnboardingViewModel)
}
