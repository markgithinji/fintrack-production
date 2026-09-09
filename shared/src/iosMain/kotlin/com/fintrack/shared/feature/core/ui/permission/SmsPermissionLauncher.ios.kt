package com.fintrack.shared.feature.core.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun SmsPermissionLauncher(
    trigger: Boolean,
    onResult: (Boolean) -> Unit,
    onDismissTrigger: () -> Unit
) {
    LaunchedEffect(trigger) {
        if (trigger) {
            onResult(false) // SMS reading not supported on iOS
            onDismissTrigger()
        }
    }
}

@Composable
actual fun rememberSmsPermissionState(): Boolean = false
