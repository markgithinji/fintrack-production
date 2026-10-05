package com.fintrack.shared.feature.budget.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.core.domain.ValidationResult
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetValidationUseCaseTest {

    private val validator = BudgetValidationUseCase()

    private val sampleCategory = Category(id = "c1", name = "Food", isExpense = true)
    private val sampleAccount = Account(id = "a1", name = "M-Pesa")
    private val startDate = LocalDate(2026, 1, 1)
    private val endDate = LocalDate(2026, 1, 31)

    @Test
    fun `when name is blank returns error`() {
        val result = validator(
            name = "",
            amount = "1000",
            categories = setOf(sampleCategory),
            startDate = startDate,
            endDate = endDate,
            selectedAccounts = setOf(sampleAccount)
        )
        assertEquals(ValidationResult.Error("Budget name is required"), result)
    }

    @Test
    fun `when amount is zero or negative returns error`() {
        val result = validator(
            name = "Food Budget",
            amount = "0",
            categories = setOf(sampleCategory),
            startDate = startDate,
            endDate = endDate,
            selectedAccounts = setOf(sampleAccount)
        )
        assertEquals(ValidationResult.Error("Valid amount is required"), result)
    }

    @Test
    fun `when accounts are empty returns error`() {
        val result = validator(
            name = "Food Budget",
            amount = "1000",
            categories = setOf(sampleCategory),
            startDate = startDate,
            endDate = endDate,
            selectedAccounts = emptySet()
        )
        assertEquals(ValidationResult.Error("Please select at least one account for this budget"), result)
    }

    @Test
    fun `when categories are empty returns error`() {
        val result = validator(
            name = "Food Budget",
            amount = "1000",
            categories = emptySet(),
            startDate = startDate,
            endDate = endDate,
            selectedAccounts = setOf(sampleAccount)
        )
        assertEquals(ValidationResult.Error("At least one category is required"), result)
    }

    @Test
    fun `when start date after end date returns error`() {
        val result = validator(
            name = "Food Budget",
            amount = "1000",
            categories = setOf(sampleCategory),
            startDate = LocalDate(2026, 2, 1),
            endDate = LocalDate(2026, 1, 1),
            selectedAccounts = setOf(sampleAccount)
        )
        assertEquals(ValidationResult.Error("Start date cannot be after end date"), result)
    }

    @Test
    fun `when all inputs are valid returns success`() {
        val result = validator(
            name = "Food Budget",
            amount = "1000",
            categories = setOf(sampleCategory),
            startDate = startDate,
            endDate = endDate,
            selectedAccounts = setOf(sampleAccount)
        )
        assertEquals(ValidationResult.Success, result)
    }
}
