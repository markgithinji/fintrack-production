package com.fintrack.shared.feature.core.ui.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberHapticFeedback(): () -> Unit
