package com.example.digitaljourney.model

import android.net.Uri

sealed class LogEntry(val timestamp: Long) {
    data class LocationLog(
        val lat: Double,
        val lon: Double,
        val address: String?,
        val time: Long
    ) : LogEntry(time)

    data class SpotifyLog(
        val song: String,
        val artist: String,
        val time: Long
    ) : LogEntry(time)

    data class PhotoLog(
        val uri: Uri,
        val time: Long
    ) : LogEntry(time)

    data class MoodLog(
        val mood: String,
        val time: Long
    ) : LogEntry(time)

    data class ChessLog(
        val primaryText: String,
        val secondaryText: String,
        val time: Long
    ) : LogEntry(time)
}
