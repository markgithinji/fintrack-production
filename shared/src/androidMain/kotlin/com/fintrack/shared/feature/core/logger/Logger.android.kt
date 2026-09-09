package com.fintrack.shared.feature.core.logger

import android.util.Log

actual class KMPLogger actual constructor() {
    actual fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    actual fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    actual fun warning(tag: String, message: String) {
        Log.w(tag, message)
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}