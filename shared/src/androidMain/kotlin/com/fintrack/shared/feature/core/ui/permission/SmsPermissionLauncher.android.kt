package com.fintrack.shared.feature.core.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

@Composable
actual fun SmsPermissionLauncher(
    trigger: Boolean,
    onResult: (Boolean) -> Unit,
    onDismissTrigger: () -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val readGranted = permissions[Manifest.permission.READ_SMS] == true
            onResult(readGranted)
        }
    )

    LaunchedEffect(trigger) {
        if (trigger) {
            val currentReadStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)

            if (currentReadStatus == PackageManager.PERMISSION_GRANTED) {
                onResult(true)
                return@LaunchedEffect
            }

            // Small delay to ensure any preceding dialog dismissals or window transitions are settled
            delay(300)
            
            try {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            } catch (_: Exception) {
                onDismissTrigger()
            }
        }
    }
}

@Composable
actual fun rememberSmsPermissionState(): Boolean {
    val context = LocalContext.current
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED
}
