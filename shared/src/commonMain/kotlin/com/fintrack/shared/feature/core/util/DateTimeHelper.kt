package com.fintrack.shared.feature.core.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateTimeHelper {
    fun now(): Instant {
        return Clock.System.now()
    }

    fun today(): String {
        return now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }

    fun currentMonthCode(): String {
        val now = now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        @Suppress("DEPRECATION")
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
    }
}
