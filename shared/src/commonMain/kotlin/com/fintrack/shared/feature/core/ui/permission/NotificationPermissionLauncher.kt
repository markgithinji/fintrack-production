package com.fintrack.shared.feature.core.ui.permission

import androidx.compose.runtime.Composable

@Composable
expect fun NotificationPermissionLauncher(
    trigger: Boolean,
    onResult: (Boolean) -> Unit,
    onDismissTrigger: () -> Unit
)
