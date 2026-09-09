package com.fintrack.shared.feature.core.ui.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    return remember { IOSBiometricAuthenticator() }
}
