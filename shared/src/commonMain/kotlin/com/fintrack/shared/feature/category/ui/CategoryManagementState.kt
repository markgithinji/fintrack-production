package com.fintrack.shared.feature.category.ui

import com.fintrack.shared.feature.category.domain.model.Category

data class CategoryManagementState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)