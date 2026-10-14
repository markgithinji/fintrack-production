package com.fintrack.shared.feature.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

@Composable
actual fun rememberHapticFeedback(): () -> Unit {
    val generator = remember {
        UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    }
    return remember(generator) {
        {
            generator.prepare()
            generator.impactOccurred()
        }
    }
}
