package com.fintrack.shared.feature.core.di

import com.fintrack.shared.feature.auth.data.local.IOSTokenDataSource
import com.fintrack.shared.feature.auth.domain.datasource.TokenDataSource
import com.fintrack.shared.feature.core.domain.service.NotificationService
import com.fintrack.shared.feature.core.service.IOSNotificationService
import com.fintrack.shared.feature.core.util.FileSaver
import com.fintrack.shared.feature.core.util.IosFileSaver
import com.fintrack.shared.feature.settings.data.local.IOSSettingsDataSource
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.transaction.domain.service.TransactionImporter
import com.fintrack.shared.feature.transaction.service.IosTransactionImporter
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: org.koin.core.module.Module = module {
    single { IOSTokenDataSource() } bind TokenDataSource::class
    single { IOSSettingsDataSource() } bind SettingsDataSource::class
    single { IOSNotificationService(get()) } bind NotificationService::class
    single { IosFileSaver() } bind FileSaver::class
    single { IosTransactionImporter() } bind TransactionImporter::class
}
