package com.fintrack.shared.feature.category.util

import com.fintrack.shared.feature.category.domain.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryMatcherTest {

    private val categories = listOf(
        Category(id = "cat_groceries", name = "Groceries", isExpense = true),
        Category(id = "cat_utilities", name = "Utilities", isExpense = true),
        Category(id = "cat_transport", name = "Transport", isExpense = true)
    )

    @Test
    fun `matches hardcoded keyword for groceries`() {
        val matchedId = CategoryMatcher.resolveCategory(
            inputCategoryId = null,
            inputCategoryName = null,
            description = "Naivas Supermarket Purchase",
            isIncome = false,
            allCategories = categories
        )
        assertEquals("cat_groceries", matchedId)
    }

    @Test
    fun `matches hardcoded keyword for utilities`() {
        val matchedId = CategoryMatcher.resolveCategory(
            inputCategoryId = null,
            inputCategoryName = null,
            description = "KPLC Prepaid Token",
            isIncome = false,
            allCategories = categories
        )
        assertEquals("cat_utilities", matchedId)
    }
}
