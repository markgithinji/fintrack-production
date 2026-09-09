package com.fintrack.shared.feature.user.domain.model

data class User(
    val name: String,
    val email: String,
    val trackedCategoryIds: List<String> = emptyList()
)
