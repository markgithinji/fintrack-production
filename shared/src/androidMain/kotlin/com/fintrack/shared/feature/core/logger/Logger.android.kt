package com.fintrack.shared.feature.core.logger

import android.util.Log

actual class KMPLogger actual constructor() {
    actual fun debug(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
            println("[$tag] $message")
        }
    }

    actual fun info(tag: String, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: Throwable) {
            println("[$tag] $message")
        }
    }

    actual fun warning(tag: String, message: String) {
        try {
            Log.w(tag, message)
        } catch (_: Throwable) {
            println("[$tag] $message")
        }
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        try {
            Log.e(tag, message, throwable)
        } catch (_: Throwable) {
            println("[$tag] ERROR: $message ${throwable?.message ?: ""}")
        }
    }
}
