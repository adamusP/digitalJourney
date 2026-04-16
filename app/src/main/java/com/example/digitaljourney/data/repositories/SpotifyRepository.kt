package com.example.digitaljourney.data.repositories

import com.example.digitaljourney.model.LogEntry
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed class SpotifyFetchResult {
    data class Success(val logs: List<LogEntry.SpotifyLog>) : SpotifyFetchResult()
    data object Unauthorized : SpotifyFetchResult()
    data object Error : SpotifyFetchResult()
}

interface SpotifyRepository {

    suspend fun fetchRecentlyPlayedBlocking(
        token: String
    ): SpotifyFetchResult
}


class SpotifyRepositoryImpl : SpotifyRepository {
    private val client = OkHttpClient()

    override suspend fun fetchRecentlyPlayedBlocking(token: String): SpotifyFetchResult {

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/recently-played?limit=50")
            .addHeader("Authorization", "Bearer $token")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.code == 401) {
                    return SpotifyFetchResult.Unauthorized
                }

                if (!response.isSuccessful) {
                    return SpotifyFetchResult.Error
                }

                val logs = mutableListOf<LogEntry.SpotifyLog>()
                val today = LocalDate.now()

                val body = response.body?.string() ?: return SpotifyFetchResult.Success(emptyList())
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

                SpotifyFetchResult.Success(logs)
            }
        } catch (e: IOException) {
            SpotifyFetchResult.Error
        } catch (e: Exception) {
            SpotifyFetchResult.Error
        }
    }

}