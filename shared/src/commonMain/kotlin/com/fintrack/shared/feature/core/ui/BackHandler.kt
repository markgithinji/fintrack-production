package com.fintrack.shared.feature.core.ui

import androidx.compose.runtime.Composable

@Composable
expect fun KMPBackHandler(enabled: Boolean = true, onBack: () -> Unit)
