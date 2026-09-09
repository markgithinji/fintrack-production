package com.fintrack.shared.feature.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import kotlin.time.Clock

/**
 * A utility to prevent multiple rapid clicks on the same UI element.
 * 
 * @param threshold The time in milliseconds to ignore subsequent clicks.
 * @param onClick The original click handler.
 */
@Composable
fun rememberThrottleClick(
    threshold: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    val lastClick = remember { object { var time = 0L } }
    
    return remember(onClick, threshold) {
        {
            val currentTime = Clock.System.now().toEpochMilliseconds()
            if (currentTime - lastClick.time > threshold) {
                lastClick.time = currentTime
                onClick()
            }
        }
    }
}

/**
 * A utility to prevent multiple rapid clicks for navigation or action handlers.
 * Similar to rememberThrottleClick but takes arguments.
 */
@Composable
fun <T> rememberThrottleClick(
    threshold: Long = 500L,
    onClick: (T) -> Unit
): (T) -> Unit {
    val lastClick = remember { object { var time = 0L } }
    
    return remember(onClick, threshold) {
        { arg ->
            val currentTime = Clock.System.now().toEpochMilliseconds()
            if (currentTime - lastClick.time > threshold) {
                lastClick.time = currentTime
                onClick(arg)
            }
        }
    }
}

/**
 * Navigates safely by preventing multiple rapid calls to the same destination.
 */
fun NavController.navigateThrottled(
    route: Any,
    threshold: Long = 500L,
    builder: androidx.navigation.NavOptionsBuilder.() -> Unit = {}
) {
    val lastNavigateTime = NavigationThrottle.lastTime
    val currentTime = Clock.System.now().toEpochMilliseconds()
    
    if (currentTime - lastNavigateTime > threshold) {
        NavigationThrottle.lastTime = currentTime
        this.navigate(route, builder)
    }
}

private object NavigationThrottle {
    var lastTime = 0L
}
