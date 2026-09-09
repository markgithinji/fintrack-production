package com.fintrack.shared.feature.core.ui.permission

import androidx.compose.runtime.Composable

@Composable
expect fun SmsPermissionLauncher(
    trigger: Boolean,
    onResult: (Boolean) -> Unit,
    onDismissTrigger: () -> Unit
)

@Composable
expect fun rememberSmsPermissionState(): Boolean
