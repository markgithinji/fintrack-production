package com.fintrack.shared.feature.summary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AvailableMonthsDto(
    val months: List<String> // e.g., ["2025-07", "2025-08", "2025-09"]
)