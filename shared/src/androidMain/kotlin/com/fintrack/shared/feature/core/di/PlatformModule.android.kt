package com.fintrack.shared.feature.core.di

import com.fintrack.shared.feature.auth.data.local.AndroidTokenDataSource
import com.fintrack.shared.feature.auth.domain.datasource.TokenDataSource
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.service.AndroidNotificationService
import com.fintrack.shared.feature.core.util.AndroidFileSaver
import com.fintrack.shared.feature.core.util.FileSaver
import com.fintrack.shared.feature.settings.data.local.AndroidSettingsDataSource
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter
import com.fintrack.shared.feature.transaction.service.importer.AndroidTransactionImporter
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: org.koin.core.module.Module = module {
    single { AndroidTokenDataSource(context = get()) } bind TokenDataSource::class
    single { AndroidSettingsDataSource(get()) } bind SettingsDataSource::class
    single {
        AndroidNotificationService(
            context = get(),
            settingsDataSource = get()
        )
    } bind NotificationService::class
    single { AndroidFileSaver(context = get()) } bind FileSaver::class
    single {
        AndroidTransactionImporter(
            context = get(),
            transactionRepository = get(),
            accountRepository = get(),
            categoryRepository = get(),
            settingsDataSource = get()
        )
    } bind TransactionImporter::class
}
