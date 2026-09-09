package com.fintrack.shared.feature.budget.domain.model

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.category.domain.model.Category
import kotlinx.datetime.LocalDate

data class BudgetFormState(
    val id: String? = null,
    val name: String = "",
    val amount: String = "",
    val amountSelectionStart: Int = 0,
    val amountSelectionEnd: Int = 0,
    val selectedCategories: Set<Category> = emptySet(),
    val isExpense: Boolean = true,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val selectedAccounts: Set<Account> = emptySet()
)