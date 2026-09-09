package com.fintrack.shared.feature.core.ui.biometric

import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi

class IOSBiometricAuthenticator : BiometricAuthenticator {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun authenticate(
        title: String,
        subtitle: String
    ): BiometricResult {
        val context = LAContext()
        
        // Check if biometric is available
        val canEvaluate = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
        
        if (!canEvaluate) {
            return BiometricResult.NotAvailable
        }

        return suspendCancellableCoroutine { continuation ->
            context.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthentication,
                localizedReason = subtitle
            ) { success, error ->
                if (success) {
                    continuation.resume(BiometricResult.Success)
                } else {
                    val message = error?.localizedDescription ?: "Biometric authentication failed"
                    continuation.resume(BiometricResult.Error(message))
                }
            }
        }
    }
}
