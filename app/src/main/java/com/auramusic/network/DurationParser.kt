package com.auramusic.network

object DurationParser {
    fun parse(iso8601: String?): Long {
        if (iso8601.isNullOrBlank()) return 0L
        val value = iso8601.uppercase()
        val days = extract(value, 'D')
        val hours = extract(value, 'H')
        val minutes = extract(value, 'M')
        val seconds = extract(value, 'S')
        return (((days * 24 + hours) * 60 + minutes) * 60 + seconds) * 1000L
    }

    private fun extract(value: String, unit: Char): Long =
        Regex("(\\d+)$unit").find(value)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
}

fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
