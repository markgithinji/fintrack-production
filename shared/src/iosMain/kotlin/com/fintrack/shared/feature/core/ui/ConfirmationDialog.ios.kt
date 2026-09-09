package com.fintrack.shared.feature.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.interop.LocalUIViewController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertActionStyleDestructive
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert

@Composable
actual fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    isDestructive: Boolean,
    isLoading: Boolean,
    isSuccess: Boolean,
    errorMessage: String?,
    successTitle: String?,
    successMessage: String?,
    autoDismiss: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val viewController = LocalUIViewController.current

    LaunchedEffect(isLoading, isSuccess, errorMessage) {
        if (isSuccess) {
             val successAlert = UIAlertController.alertControllerWithTitle(
                title = successTitle ?: "Success!",
                message = successMessage ?: "Action completed successfully.",
                preferredStyle = UIAlertControllerStyleAlert
            )
            successAlert.addAction(
                UIAlertAction.actionWithTitle(
                    title = "OK",
                    style = UIAlertActionStyleDefault,
                    handler = { onDismiss() }
                )
            )
            viewController.presentViewController(successAlert, animated = true, completion = null)
            return@LaunchedEffect
        }

        if (errorMessage != null) {
            val errorAlert = UIAlertController.alertControllerWithTitle(
                title = "Operation Failed",
                message = errorMessage,
                preferredStyle = UIAlertControllerStyleAlert
            )
            errorAlert.addAction(
                UIAlertAction.actionWithTitle(
                    title = "Close",
                    style = UIAlertActionStyleDefault,
                    handler = { onDismiss() }
                )
            )
            viewController.presentViewController(errorAlert, animated = true, completion = null)
            return@LaunchedEffect
        }

        if (isLoading) return@LaunchedEffect

        val alertController = UIAlertController.alertControllerWithTitle(
            title = title,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert
        )

        alertController.addAction(
            UIAlertAction.actionWithTitle(
                title = confirmLabel,
                style = if (isDestructive) UIAlertActionStyleDestructive else UIAlertActionStyleDefault,
                handler = {
                    onConfirm()
                    if (autoDismiss) onDismiss()
                }
            )
        )

        alertController.addAction(
            UIAlertAction.actionWithTitle(
                title = cancelLabel,
                style = UIAlertActionStyleCancel,
                handler = {
                    onDismiss()
                }
            )
        )

        viewController.presentViewController(alertController, animated = true, completion = null)
    }
}
