package com.fintrack.shared.feature.user.domain.repository

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.user.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    fun getUserProfile(): StateFlow<User?>
    suspend fun refreshProfile(): Result<User>
    suspend fun updateProfile(name: String, email: String): Result<User>
    suspend fun updateTrackedCategories(categories: List<String>): Result<User>
    suspend fun deleteAccount(): Result<Unit>
    fun clearProfile()
}
