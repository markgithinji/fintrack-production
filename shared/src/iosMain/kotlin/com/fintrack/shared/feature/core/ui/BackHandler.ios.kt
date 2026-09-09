package com.fintrack.shared.feature.core.ui

import androidx.compose.runtime.Composable

@Composable
actual fun KMPBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for iOS as it doesn't have a hardware/system back button
}
