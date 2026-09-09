package com.fintrack.shared.feature.user.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackedCategoriesRequest(
    val categoryIds: List<String>
)
