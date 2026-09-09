package com.fintrack.shared.feature.core.ui

import androidx.compose.runtime.Composable

@Composable
expect fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String = "Cancel",
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    errorMessage: String? = null,
    successTitle: String? = null,
    successMessage: String? = null,
    autoDismiss: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
)
