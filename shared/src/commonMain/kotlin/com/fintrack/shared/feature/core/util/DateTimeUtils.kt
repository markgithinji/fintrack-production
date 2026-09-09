package com.fintrack.shared.feature.core.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

object DateTimeUtils {
    /**
     * Formats an Instant to a relative time string (e.g., "Just now", "5 mins ago", "2 hours ago").
     */
    fun toRelativeDateTimeString(instant: Instant?): String {
        if (instant == null) return "Never synced"

        val now = Clock.System.now()
        val duration = now - instant

        return when {
            duration < 1.minutes -> "Just now"
            duration < 60.minutes -> "${duration.inWholeMinutes} mins ago"
            duration < 24.hours -> "${duration.inWholeHours} hours ago"
            duration < 2.days -> "Yesterday"
            else -> {
                val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${dateTime.day}/${dateTime.month.number}/${dateTime.year}"
            }
        }
    }

    /**
     * Calculates the start and end dates for a given ISO week code (e.g., "2024-W25").
     * Uses the ISO 8601 standard where Week 1 is the week with the first Thursday of the year.
     * This is equivalent to the week containing January 4th.
     */
    fun getIsoWeekRange(code: String): Pair<LocalDate, LocalDate>? {
        return try {
            val parts = code.split("-W")
            if (parts.size != 2) return null

            val year = parts[0].toIntOrNull() ?: return null
            val week = parts[1].toIntOrNull() ?: return null

            // ISO 8601 rule: Find Jan 4th of the year (always in Week 1)
            val jan4 = LocalDate(year, 1, 4)
            val dayOfWeek = jan4.dayOfWeek.isoDayNumber // Mon=1, Sun=7

            // Find the Monday of that week
            val mondayOfWeek1 = jan4.plus(DatePeriod(days = -(dayOfWeek - 1)))

            val weekStart = mondayOfWeek1.plus(DatePeriod(days = (week - 1) * 7))
            val weekEnd = weekStart.plus(DatePeriod(days = 6))

            weekStart to weekEnd
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculates the start and end dates for a given month code (e.g., "2024-06").
     */
    fun getMonthRange(code: String): Pair<LocalDate, LocalDate>? {
        return try {
            val parts = code.split("-")
            if (parts.size != 2) return null

            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val start = LocalDate(year, month, 1)
            val end = if (month == 12) {
                LocalDate(year + 1, 1, 1).plus(DatePeriod(days = -1))
            } else {
                LocalDate(year, month + 1, 1).plus(DatePeriod(days = -1))
            }
            start to end
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats a month code (e.g., "2024-06") to a human-readable string (e.g., "Jun 2024").
     */
    fun formatMonthCode(code: String): String {
        return try {
            val parts = code.split("-")
            if (parts.size == 2) {
                val year = parts[0]
                val monthNumber = parts[1].toIntOrNull() ?: return code
                val monthName = when (monthNumber) {
                    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                    else -> ""
                }
                "$monthName $year"
            } else code
        } catch (_: Exception) {
            code
        }
    }
}
