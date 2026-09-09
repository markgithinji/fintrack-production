package com.fintrack.shared.feature.core.ui.biometric

import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val context = LocalContext.current
    return remember(context) {
        val activity = context as? FragmentActivity 
            ?: (context as? ContextWrapper)?.baseContext as? FragmentActivity
            ?: error("Context must be FragmentActivity. Current context: $context")
        AndroidBiometricAuthenticator(activity)
    }
}
