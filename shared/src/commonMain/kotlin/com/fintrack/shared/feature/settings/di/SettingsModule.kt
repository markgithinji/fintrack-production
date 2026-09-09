package com.fintrack.shared.feature.settings.di

import com.fintrack.shared.feature.settings.ui.SettingsViewModel
import com.fintrack.shared.feature.settings.domain.usecase.ClearAllUserDataUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::ClearAllUserDataUseCase)
    viewModelOf(::SettingsViewModel)
}
