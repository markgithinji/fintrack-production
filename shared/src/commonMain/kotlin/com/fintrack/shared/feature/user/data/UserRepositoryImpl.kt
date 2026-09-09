package com.fintrack.shared.feature.user.data

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.core.util.safeApiCall
import com.fintrack.shared.feature.user.data.model.toDomain
import com.fintrack.shared.feature.user.domain.model.User
import com.fintrack.shared.feature.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserRepositoryImpl(
    private val api: UserApi
) : UserRepository {
    private val _userProfile = MutableStateFlow<User?>(null)

    override fun getUserProfile(): StateFlow<User?> = _userProfile.asStateFlow()

    override suspend fun refreshProfile(): Result<User> {
        return when (val result = safeApiCall { api.getUserProfile() }) {
            is Result.Success -> {
                val user = result.data.toDomain()
                _userProfile.value = user
                Result.Success(user)
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun updateProfile(name: String, email: String): Result<User> {
        return when (val result = safeApiCall { api.updateProfile(name, email) }) {
            is Result.Success -> {
                val user = result.data.toDomain()
                _userProfile.value = user
                Result.Success(user)
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun updateTrackedCategories(categories: List<String>): Result<User> {
        return when (val result = safeApiCall { api.updateTrackedCategories(categories) }) {
            is Result.Success -> {
                val user = result.data.toDomain()
                _userProfile.value = user
                Result.Success(user)
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = safeApiCall {
        api.deleteUser()
        _userProfile.value = null
    }

    override fun clearProfile() {
        _userProfile.value = null
    }
}
