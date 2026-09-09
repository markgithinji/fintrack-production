package com.fintrack.shared.feature.auth.domain.usecase

import com.fintrack.shared.feature.core.domain.ValidationResult

class ChangePasswordValidationUseCase {

    operator fun invoke(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): ChangePasswordValidationResult {
        return ChangePasswordValidationResult(
            currentPasswordResult = validateCurrentPassword(currentPassword),
            newPasswordResult = validateNewPassword(newPassword),
            confirmPasswordResult = validateConfirmPassword(newPassword, confirmPassword)
        )
    }

    private fun validateCurrentPassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error("Current password is required")
            else -> ValidationResult.Success
        }
    }

    private fun validateNewPassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error("New password is required")
            password.length < 6 -> ValidationResult.Error("Password must be at least 6 characters")
            else -> ValidationResult.Success
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult.Error("Confirm password is required")
            password != confirmPassword -> ValidationResult.Error("Passwords do not match")
            else -> ValidationResult.Success
        }
    }
}

data class ChangePasswordValidationResult(
    val currentPasswordResult: ValidationResult = ValidationResult.Success,
    val newPasswordResult: ValidationResult = ValidationResult.Success,
    val confirmPasswordResult: ValidationResult = ValidationResult.Success
) {
    val isValid: Boolean = currentPasswordResult is ValidationResult.Success &&
            newPasswordResult is ValidationResult.Success &&
            confirmPasswordResult is ValidationResult.Success
}
