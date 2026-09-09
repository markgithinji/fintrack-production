package com.fintrack.shared.feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AvailableYearsDto(
    val years: List<String>
)
