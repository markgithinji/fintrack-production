package com.fintrack.shared.feature.transaction.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal

class ValidateTransactionUseCase {
    operator fun invoke(
        amount: String,
        transactionCost: String = "0",
        description: String,
        category: Category?,
        selectedAccount: Account?
    ): ValidationResult {
        val parsedAmount = try { amount.toBigDecimal() } catch (_: Exception) { null }
        val parsedCost = try { transactionCost.toBigDecimal() } catch (_: Exception) { null }

        return when {
            amount.isBlank() -> ValidationResult.Error("Please enter an amount")
            parsedAmount == null -> ValidationResult.Error("Please enter a valid amount")
            parsedAmount <= BigDecimal.ZERO -> ValidationResult.Error("Amount must be greater than zero")
            
            transactionCost.isNotBlank() && parsedCost == null -> 
                ValidationResult.Error("Please enter a valid transaction cost")
            parsedCost != null && parsedCost < BigDecimal.ZERO ->
                ValidationResult.Error("Transaction cost cannot be negative")

            category == null -> ValidationResult.Error("Please select a category")
            selectedAccount == null -> ValidationResult.Error("Please select an account")
            description.isBlank() -> ValidationResult.Error("Please enter a description")
            else -> ValidationResult.Success
        }
    }
}
