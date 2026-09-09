package com.fintrack.shared.feature.budget.domain.usecase

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.core.domain.ValidationResult
import com.fintrack.shared.feature.category.domain.model.Category
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.datetime.LocalDate

class BudgetValidationUseCase {
    operator fun invoke(
        name: String,
        amount: String,
        categories: Set<Category>,
        startDate: LocalDate?,
        endDate: LocalDate?,
        selectedAccounts: Set<Account>
    ): ValidationResult {
        val limit = try { amount.toBigDecimal() } catch (_: Exception) { BigDecimal.ZERO }

        return when {
            name.isBlank() -> ValidationResult.Error("Budget name is required")
            limit <= BigDecimal.ZERO -> ValidationResult.Error("Valid amount is required")
            selectedAccounts.isEmpty() -> ValidationResult.Error("Please select at least one account for this budget")
            categories.isEmpty() -> ValidationResult.Error("At least one category is required")
            startDate == null -> ValidationResult.Error("Start date is required")
            endDate == null -> ValidationResult.Error("End date is required")
            else -> ValidationResult.Success
        }
    }
}