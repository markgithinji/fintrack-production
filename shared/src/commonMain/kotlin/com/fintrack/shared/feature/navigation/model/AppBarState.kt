package com.fintrack.shared.feature.navigation.model

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

data class AppBarState(
    val title: String,
    val showBackButton: Boolean = false,
    val onBack: (() -> Unit)? = null,
    val actions: (@Composable RowScope.() -> Unit)? = null
)
