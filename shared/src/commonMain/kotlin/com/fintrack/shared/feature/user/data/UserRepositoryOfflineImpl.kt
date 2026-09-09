package com.fintrack.shared.feature.user.data

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.user.domain.model.User
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserRepositoryOfflineImpl : UserRepository {
    private val _userProfile = MutableStateFlow<User?>(
        User(
            name = "Offline User",
            email = "offline@fintrack.local",
            trackedCategoryIds = emptyList()
        )
    )

    override fun getUserProfile(): StateFlow<User?> = _userProfile.asStateFlow()

    override suspend fun refreshProfile(): Result<User> {
        return _userProfile.value?.let { Result.Success(it) } 
            ?: Result.Error(Exception("User not found"))
    }

    override suspend fun updateProfile(name: String, email: String): Result<User> {
        val updatedUser = _userProfile.value?.copy(name = name, email = email) ?: return Result.Error(Exception("User not found"))
        _userProfile.value = updatedUser
        return Result.Success(updatedUser)
    }

    override suspend fun updateTrackedCategories(categories: List<String>): Result<User> {
        val updatedUser = _userProfile.value?.copy(trackedCategoryIds = categories) ?: return Result.Error(Exception("User not found"))
        _userProfile.value = updatedUser
        return Result.Success(updatedUser)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return Result.Success(Unit)
    }

    override fun clearProfile() {
        // No-op for offline
    }
}
