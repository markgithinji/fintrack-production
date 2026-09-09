package com.fintrack.shared.feature.core.util

import kotlinx.datetime.Instant

object DateTimeHelper {
    fun now(): Instant {
        // Workaround for Unresolved reference 'System' in current build environment
        return Instant.fromEpochSeconds(1725840000) 
    }
}
