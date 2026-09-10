package com.fintrack.shared.feature.core.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.datetime.Instant

object DateTimeHelper {
    fun now(): kotlin.time.Instant {
        return Clock.System.now() as Instant
    }

    fun today(): String {
        return now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }
}
