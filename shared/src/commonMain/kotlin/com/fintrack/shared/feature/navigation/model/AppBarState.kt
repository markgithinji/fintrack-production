package com.fintrack.shared.feature.navigation.model

data class AppBarState(
    val title: String,
    val showBackButton: Boolean = false,
    val onBack: (() -> Unit)? = null
)
