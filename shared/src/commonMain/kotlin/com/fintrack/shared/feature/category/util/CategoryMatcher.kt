package com.fintrack.shared.feature.category.util

import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule

object CategoryMatcher {

    /**
     * Resolves a category ID based on provided inputs and existing categories.
     * Mirrors the backend fuzzy matching logic for Safaricom/Equity descriptions.
     */
    fun resolveCategory(
        inputCategoryId: String?,
        inputCategoryName: String?,
        description: String?,
        isIncome: Boolean,
        allCategories: List<Category>,
        rules: List<CategoryRule> = emptyList(),
        defaultId: String = "pending"
    ): String {
        // 1. Valid ID Check
        if (!inputCategoryId.isNullOrBlank() && inputCategoryId != "pending") {
            return inputCategoryId
        }

        val rawInput = if (inputCategoryName?.lowercase() == "pending") null else inputCategoryName
        val textToMatch = (rawInput ?: description ?: "").lowercase()
        if (textToMatch.isBlank()) return defaultId

        val isExpense = !isIncome

        // 2. Dynamic Rule Matching
        rules.filter { it.isExpense == isExpense }.forEach { rule ->
            val keyword = rule.keyword.lowercase()
            if (textToMatch.contains(keyword) || (description?.lowercase()?.contains(keyword) == true)) {
                return rule.categoryId
            }
        }

        val normalizedInput = textToMatch.replace("-", "").trim()

        // 3. Direct match with type
        allCategories.find {
            it.name.equals(textToMatch, ignoreCase = true) && it.isExpense == isExpense
        }?.let { return it.id }

        // 4. Direct match without type (fallback)
        allCategories.find {
            it.name.equals(textToMatch, ignoreCase = true)
        }?.let { return it.id }

        // 5. Fuzzy matching for variations
        allCategories.find {
            val norm = it.name.replace("-", "").trim().lowercase()
            norm == normalizedInput || norm.startsWith(normalizedInput) || normalizedInput.startsWith(norm)
        }?.let { return it.id }

        // 6. Safaricom / M-Pesa Specific Aliases (Backend Port)
        if (normalizedInput.contains("shwari") || normalizedInput.contains("saving")) {
            allCategories.find { it.name.contains("Savings", ignoreCase = true) }?.let { return it.id }
        }
        if (normalizedInput.contains("loan")) {
            allCategories.find { it.name.contains("Loans", ignoreCase = true) }?.let { return it.id }
        }

        // 7. Final Fallback
        val fallbackName = if (isExpense) "Misc" else "Other Income"
        return allCategories.find { it.name.equals(fallbackName, ignoreCase = true) }?.id
            ?: allCategories.find { it.isExpense == isExpense }?.id
            ?: allCategories.firstOrNull { it.isDefault }?.id
            ?: allCategories.firstOrNull()?.id
            ?: defaultId
    }
}
