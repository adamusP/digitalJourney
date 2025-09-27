// com/example/digitaljourney/data/SpotifyRepository.kt
package com.example.digitaljourney.data

import com.example.digitaljourney.model.LogEntry
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


interface SpotifyRepository {
    fun fetchTodayLogs(
        token: String,
        onResult: (List<LogEntry.SpotifyLog>) -> Unit
    )

    suspend fun fetchRecentlyPlayedBlocking(
        token: String
    ): List<LogEntry.SpotifyLog>
}


class SpotifyRepositoryImpl : SpotifyRepository {
    private val client = OkHttpClient()

    override fun fetchTodayLogs(token: String, onResult: (List<LogEntry.SpotifyLog>) -> Unit) {
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/recently-played?limit=50")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val logs = mutableListOf<LogEntry.SpotifyLog>()
                val today = LocalDate.now()

                response.body?.string()?.let { json ->
                    val items = JSONObject(json).getJSONArray("items")
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val playedAt = Instant.parse(item.getString("played_at"))
                        val playedDate = playedAt.atZone(ZoneId.systemDefault()).toLocalDate()

                        if (playedDate == today) {
                            val track = item.getJSONObject("track")
                            val name = track.getString("name")
                            val artist = track.getJSONArray("artists")
                                .getJSONObject(0)
                                .getString("name")
                            logs.add(LogEntry.SpotifyLog(name, artist, playedAt.toEpochMilli()))
                        }
                    }
                }
                onResult(logs)
            }
        })
    }

    override suspend fun fetchRecentlyPlayedBlocking(token: String): List<LogEntry.SpotifyLog> {
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/recently-played?limit=50")
            .addHeader("Authorization", "Bearer $token")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                android.util.Log.e("SpotifyRepo", "Spotify error: ${response.code}")
                return emptyList()
            }

            val logs = mutableListOf<LogEntry.SpotifyLog>()
            val today = LocalDate.now()

            val body = response.body?.string() ?: return emptyList()
            val items = JSONObject(body).getJSONArray("items")

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val playedAt = Instant.parse(item.getString("played_at"))
                val playedDate = playedAt.atZone(ZoneId.systemDefault()).toLocalDate()

                if (playedDate == today) {
                    val track = item.getJSONObject("track")
                    val name = track.getString("name")
                    val artist = track.getJSONArray("artists")
                        .getJSONObject(0)
                        .getString("name")
                    logs.add(LogEntry.SpotifyLog(name, artist, playedAt.toEpochMilli()))
                }
            }

            logs
        }
    }

}