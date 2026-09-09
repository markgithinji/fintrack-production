package com.fintrack.shared.feature.core.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

fun LocalDate.shortDayName(): String {
    // 0 = Monday ... 6 = Sunday
    return when (this.dayOfWeek.ordinal) {
        0 -> "Mon"; 1 -> "Tue"; 2 -> "Wed"; 3 -> "Thu"
        4 -> "Fri"; 5 -> "Sat"; 6 -> "Sun"
        else -> ""
    }
}

fun LocalDate.formatAsShortDate(): String {
    val month = when (this.month) {
        Month.JANUARY -> "Jan"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dec"
    }
    return "$month $day"
}

fun LocalDate.formatAsShortDateWithYear(): String {
    val month = when (this.month) {
        Month.JANUARY -> "Jan"
        Month.FEBRUARY -> "Feb"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"
        Month.MAY -> "May"
        Month.JUNE -> "Jun"
        Month.JULY -> "Jul"
        Month.AUGUST -> "Aug"
        Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"
        Month.NOVEMBER -> "Nov"
        Month.DECEMBER -> "Dec"
    }
    return "$month $day, ${this.year}"
}

fun LocalDate.formatAsHeaderDate(): String {
    val dayName = this.shortDayName()
    val monthName = when (this.month) {
        Month.JANUARY -> "January"
        Month.FEBRUARY -> "February"
        Month.MARCH -> "March"
        Month.APRIL -> "April"
        Month.MAY -> "May"
        Month.JUNE -> "June"
        Month.JULY -> "July"
        Month.AUGUST -> "August"
        Month.SEPTEMBER -> "September"
        Month.OCTOBER -> "October"
        Month.NOVEMBER -> "November"
        Month.DECEMBER -> "December"
    }
    return "$dayName, $monthName $day, ${this.year}"
}