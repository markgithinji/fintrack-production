package com.fintrack.shared.feature.category.ui

import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule

data class CategoryManagementState(
    val categories: List<Category> = emptyList(),
    val rules: List<CategoryRule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)