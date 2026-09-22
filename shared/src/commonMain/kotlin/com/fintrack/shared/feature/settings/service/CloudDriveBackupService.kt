package com.fintrack.shared.feature.settings.service

import com.fintrack.shared.feature.core.util.Result

interface CloudDriveBackupService {
    suspend fun uploadBackup(jsonContent: String): Result<String>
    suspend fun downloadLatestBackup(): Result<String>
}
