package com.fintrack.shared.feature.core.logger

actual class KMPLogger actual constructor() {
    actual fun debug(tag: String, message: String) {
        println("DEBUG: $tag: $message")
    }

    actual fun info(tag: String, message: String) {
        println("INFO: $tag: $message")
    }

    actual fun warning(tag: String, message: String) {
        println("WARNING: $tag: $message")
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        println("ERROR: $tag: $message")
        throwable?.printStackTrace()
    }
}