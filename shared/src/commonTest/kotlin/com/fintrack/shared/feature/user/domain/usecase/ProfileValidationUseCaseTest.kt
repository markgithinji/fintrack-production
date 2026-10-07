package com.fintrack.shared.feature.user.domain.usecase

import com.fintrack.shared.feature.core.domain.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileValidationUseCaseTest {

    private val validator = ProfileValidationUseCase()

    @Test
    fun `valid name and email returns success`() {
        val result = validator("John Doe", "john@example.com")
        assertTrue(result.isValid)
        assertEquals(ValidationResult.Success, result.nameResult)
        assertEquals(ValidationResult.Success, result.emailResult)
    }

    @Test
    fun `blank name returns error`() {
        val result = validator("", "john@example.com")
        assertFalse(result.isValid)
        assertEquals(ValidationResult.Error("Name cannot be empty."), result.nameResult)
    }

    @Test
    fun `invalid email returns error`() {
        val result = validator("John Doe", "invalid-email")
        assertFalse(result.isValid)
        assertEquals(ValidationResult.Error("Please enter a valid email address."), result.emailResult)
    }
}
