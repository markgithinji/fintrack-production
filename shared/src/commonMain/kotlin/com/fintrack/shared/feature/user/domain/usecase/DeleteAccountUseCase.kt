package com.fintrack.shared.feature.user.domain.usecase

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.settings.domain.datasource.SettingsDataSource
import com.fintrack.shared.feature.user.domain.repository.UserRepository

class DeleteAccountUseCase(
    private val userRepository: UserRepository,
    private val settingsDataSource: SettingsDataSource
) {
    suspend operator fun invoke(): Result<Unit> {
        val deleteResult = userRepository.deleteAccount()
        if (deleteResult is Result.Error) return deleteResult

        // Clear ALL local data
        settingsDataSource.clear()
        userRepository.clearProfile()

        return Result.Success(Unit)
    }
}
