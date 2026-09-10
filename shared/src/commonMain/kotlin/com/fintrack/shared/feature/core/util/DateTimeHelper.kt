package com.fintrack.shared.feature.core.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object DateTimeHelper {
    fun now(): Instant {
        return Clock.System.now() as Instant
    }

    fun today(): String {
        return now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }
}
