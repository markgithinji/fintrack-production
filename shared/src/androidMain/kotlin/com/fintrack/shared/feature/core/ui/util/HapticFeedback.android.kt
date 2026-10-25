package com.fintrack.shared.feature.core.ui.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberHapticFeedback(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            performAndroidHapticClick(context, heavy = false)
        }
    }
}

@Composable
actual fun rememberLongPressHapticFeedback(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            performAndroidHapticClick(context, heavy = true)
        }
    }
}

@SuppressLint("MissingPermission")
private fun performAndroidHapticClick(context: Context, heavy: Boolean = false) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(
                if (heavy) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_CLICK
            )
            vibrator.vibrate(effect)
        } else {
            val duration = if (heavy) 50L else 15L
            val effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        }
    } catch (_: Exception) {
        // Silently ignore if unsupported
    }
}
