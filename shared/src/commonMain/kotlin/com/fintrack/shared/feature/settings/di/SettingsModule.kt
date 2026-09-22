package com.fintrack.shared.feature.settings.di

import com.fintrack.shared.feature.settings.domain.usecase.BackupRestoreUseCase
import com.fintrack.shared.feature.settings.domain.usecase.ClearAllUserDataUseCase
import com.fintrack.shared.feature.settings.service.CloudDriveBackupService
import com.fintrack.shared.feature.settings.ui.SettingsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::ClearAllUserDataUseCase)
    singleOf(::BackupRestoreUseCase)
    viewModel {
        SettingsViewModel(
            settingsDataSource = get(),
            exportTransactionsUseCase = get(),
            notificationService = get(),
            deleteAccountUseCase = get(),
            userRepository = get(),
            localCategoryDataSource = get(),
            syncCategoriesUseCase = get(),
            accountRepository = get(),
            budgetRepository = get(),
            transactionImporter = get(),
            backupRestoreUseCase = get(),
            cloudDriveBackupService = getOrNull<CloudDriveBackupService>()
        )
    }
}
