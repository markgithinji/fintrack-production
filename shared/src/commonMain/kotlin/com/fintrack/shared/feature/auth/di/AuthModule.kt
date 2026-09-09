package com.fintrack.shared.feature.auth.di

import com.fintrack.shared.feature.auth.ui.AuthViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    viewModelOf(::AuthViewModel)
}
