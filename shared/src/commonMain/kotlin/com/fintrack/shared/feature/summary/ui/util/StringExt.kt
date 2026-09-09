package com.fintrack.shared.feature.summary.ui.util

// month conversion: "yyyy-MM" -> "Jan 2025"
fun String.toMonthName(): String {
    val parts = this.split("-")
    if (parts.size != 2) return this
    val year = parts[0]
    val monthIndex = (parts[1].toIntOrNull()?.minus(1)) ?: return this
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val monthName = monthNames.getOrElse(monthIndex) { parts[1] }
    return "$monthName $year"
}

// date conversion: "yyyy-MM-dd" -> "15 Jan 2025"
fun String.toFormattedDate(): String {
    val parts = this.split("-")
    if (parts.size != 3) return this
    val day = parts[2].toIntOrNull() ?: return this
    val monthIndex = (parts[1].toIntOrNull()?.minus(1)) ?: return this
    val year = parts[0]
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val monthName = monthNames.getOrElse(monthIndex) { parts[1] }
    return "$day $monthName $year"
}