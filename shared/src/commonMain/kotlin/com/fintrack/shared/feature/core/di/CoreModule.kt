package com.fintrack.shared.feature.core.di

import com.fintrack.shared.feature.core.logger.KMPLogger
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule = module {
    singleOf(::KMPLogger)
}
