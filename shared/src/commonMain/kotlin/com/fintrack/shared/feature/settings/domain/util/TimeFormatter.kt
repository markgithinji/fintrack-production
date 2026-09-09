package com.fintrack.shared.feature.settings.domain.util

import com.fintrack.shared.feature.settings.domain.model.TimeFormat
import kotlinx.datetime.LocalTime

fun LocalTime.format(timeFormat: TimeFormat): String {
    return if (timeFormat == TimeFormat.TWENTY_FOUR_HOUR) {
        val h = hour.toString().padStart(2, '0')
        val m = minute.toString().padStart(2, '0')
        "$h:$m"
    } else {
        val amPm = if (hour < 12) "AM" else "PM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val m = minute.toString().padStart(2, '0')
        "$h12:$m $amPm"
    }
}
