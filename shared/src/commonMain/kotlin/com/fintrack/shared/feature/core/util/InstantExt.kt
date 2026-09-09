package com.fintrack.shared.feature.core.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration

fun Instant.toRelativeString(): String {
    val now = Clock.System.now()
    val diff: Duration = now - this
    
    val seconds = diff.inWholeSeconds
    val minutes = diff.inWholeMinutes
    val hours = diff.inWholeHours
    val days = diff.inWholeDays
    
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "Over a week ago"
    }
}
