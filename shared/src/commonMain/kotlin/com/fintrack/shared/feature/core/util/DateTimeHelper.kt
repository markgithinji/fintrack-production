package com.fintrack.shared.feature.core.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object DateTimeHelper {
    fun now(): Instant {
        return Clock.System.now()
    }

    fun today(): String {
        return now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }

    fun currentYearCode(): String {
        val now = now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return now.year.toString()
    }

    fun currentMonthCode(): String {
        val now = now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        @Suppress("DEPRECATION")
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
    }

    fun currentSqliteWeekCode(): String {
        val now = now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val year = now.year
        val jan1 = LocalDate(year, 1, 1)
        val dayOfWeekJan1 = jan1.dayOfWeek.isoDayNumber // Mon=1, Sun=7
        val daysToFirstMonday = if (dayOfWeekJan1 == 1) 0 else (8 - dayOfWeekJan1)
        val firstMonday = jan1.plus(DatePeriod(days = daysToFirstMonday))
        
        val weekNum = if (now < firstMonday) {
            0
        } else {
            val daysSinceFirstMonday = now.toEpochDays() - firstMonday.toEpochDays()
            (daysSinceFirstMonday / 7) + 1
        }
        return "$year-W${weekNum.toString().padStart(2, '0')}"
    }
}
