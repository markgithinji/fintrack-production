package com.fintrack.shared.feature.settings.domain.model

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun fromName(name: String?): AppTheme {
            return entries.find { it.name == name } ?: SYSTEM
        }
    }
}
