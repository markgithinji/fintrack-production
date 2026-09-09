package com.fintrack.shared.feature.auth.domain.usecase

import com.fintrack.shared.feature.core.domain.ValidationResult

class LoginValidationUseCase {

    operator fun invoke(email: String, password: String): LoginValidationResult {
        return LoginValidationResult(
            emailResult = validateEmail(email),
            passwordResult = validatePassword(password)
        )
    }

    private fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Error("Email is required")
            !isValidEmail(email) -> ValidationResult.Error("Invalid email format")
            else -> ValidationResult.Success
        }
    }

    private fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error("Password is required")
            else -> ValidationResult.Success
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.matches(emailRegex.toRegex())
    }
}

data class LoginValidationResult(
    val emailResult: ValidationResult,
    val passwordResult: ValidationResult
) {
    val isValid: Boolean = emailResult is ValidationResult.Success &&
            passwordResult is ValidationResult.Success
}
