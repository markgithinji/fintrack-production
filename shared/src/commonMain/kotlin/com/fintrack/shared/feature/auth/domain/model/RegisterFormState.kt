package com.fintrack.shared.feature.auth.domain.model

data class RegisterFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val activeError: String? = null,
    val passwordStrength: String = "None",
    val isFormValid: Boolean = false,
    val isNameDirty: Boolean = false,
    val isEmailDirty: Boolean = false,
    val isPasswordDirty: Boolean = false,
    val isConfirmPasswordDirty: Boolean = false
)