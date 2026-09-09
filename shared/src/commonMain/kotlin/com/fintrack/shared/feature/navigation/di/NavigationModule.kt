package com.fintrack.shared.feature.navigation.di

import com.fintrack.shared.feature.navigation.ui.MainViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val navigationModule = module {
    viewModelOf(::MainViewModel)
}
