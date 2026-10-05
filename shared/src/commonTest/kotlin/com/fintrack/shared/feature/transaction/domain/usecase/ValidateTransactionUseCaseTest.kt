package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateTransactionUseCaseTest {

    private val validator = ValidateTransactionUseCase()

    private val sampleCategory = Category(
        id = "cat_1",
        name = "Food",
        isExpense = true
    )

    private val sampleAccount = Account(
        id = "acc_1",
        name = "M-Pesa",
        type = AccountType.MPESA,
        balance = 1000.0.toBigDecimal()
    )

    @Test
    fun `when amount is blank returns error`() {
        val result = validator(
            amount = "",
            description = "Lunch",
            category = sampleCategory,
            selectedAccount = sampleAccount
        )
        assertEquals(ValidationResult.Error("Please enter an amount"), result)
    }

    @Test
    fun `when amount is invalid returns error`() {
        val result = validator(
            amount = "abc",
            description = "Lunch",
            category = sampleCategory,
            selectedAccount = sampleAccount
        )
        assertEquals(ValidationResult.Error("Please enter a valid amount"), result)
    }

    @Test
    fun `when amount is zero or negative returns error`() {
        val result = validator(
            amount = "0",
            description = "Lunch",
            category = sampleCategory,
            selectedAccount = sampleAccount
        )
        assertEquals(ValidationResult.Error("Amount must be greater than zero"), result)
    }

    @Test
    fun `when category is missing returns error`() {
        val result = validator(
            amount = "500",
            description = "Lunch",
            category = null,
            selectedAccount = sampleAccount
        )
        assertEquals(ValidationResult.Error("Please select a category"), result)
    }

    @Test
    fun `when account is missing returns error`() {
        val result = validator(
            amount = "500",
            description = "Lunch",
            category = sampleCategory,
            selectedAccount = null
        )
        assertEquals(ValidationResult.Error("Please select an account"), result)
    }

    @Test
    fun `when description is blank returns error`() {
        val result = validator(
            amount = "500",
            description = "",
            category = sampleCategory,
            selectedAccount = sampleAccount
        )
        assertEquals(ValidationResult.Error("Please enter a description"), result)
    }

    @Test
    fun `when all inputs are valid returns success`() {
        val result = validator(
            amount = "500",
            description = "Lunch",
            category = sampleCategory,
            selectedAccount = sampleAccount
        )
        assertEquals(ValidationResult.Success, result)
    }
}
