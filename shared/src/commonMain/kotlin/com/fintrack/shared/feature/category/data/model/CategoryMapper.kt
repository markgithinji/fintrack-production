package com.fintrack.shared.feature.category.data.model

import com.fintrack.shared.feature.category.domain.model.Category

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    name = name,
    isExpense = isExpense,
    iconName = iconName,
    isDefault = isDefault
)
