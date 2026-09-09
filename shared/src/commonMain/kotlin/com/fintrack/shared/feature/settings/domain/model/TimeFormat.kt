package com.fintrack.shared.feature.settings.domain.model

enum class TimeFormat {
    TWELVE_HOUR,
    TWENTY_FOUR_HOUR;

    companion object {
        fun fromName(name: String?): TimeFormat {
            return entries.find { it.name == name } ?: TWENTY_FOUR_HOUR
        }
    }
}
