package com.fintrack.shared.feature.auth.domain.model

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val activeError: String? = null,
    val isFormValid: Boolean = false,
    val isEmailDirty: Boolean = false,
    val isPasswordDirty: Boolean = false
)