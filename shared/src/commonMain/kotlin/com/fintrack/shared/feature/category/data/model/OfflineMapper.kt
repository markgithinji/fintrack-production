package com.fintrack.shared.feature.category.data.model

import com.fintrack.shared.db.CategoryEntity
import com.fintrack.shared.db.CategoryRuleEntity
import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        isExpense = isExpense != 0L,
        iconName = iconName,
        isDefault = isDefault != 0L
    )
}

fun CategoryRuleEntity.toDomain(): CategoryRule {
    return CategoryRule(
        id = id,
        keyword = keyword,
        categoryId = categoryId,
        isExpense = isExpense != 0L
    )
}
