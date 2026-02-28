package com.example.digitaljourney.data

import android.content.Context
import com.example.digitaljourney.model.AppDatabase
import com.example.digitaljourney.model.LogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class LogSyncManager(
    private val context: Context,
    private val spotifyRepo: SpotifyRepository,
    private val photosRepo: PhotosRepository,
    //private val callRepo: CallRepository,
    private val locationRepo: LocationRepository,
    private val weatherRepo: WeatherRepository

) {
    private val db = AppDatabase.getInstance(context)

    // calls all the repositories for the current data
    suspend fun syncNow() = withContext(Dispatchers.IO) {

        // Location
        val log = locationRepo.fetchLastKnownLocationBlocking(context)

        if (log != null) {
            db.logDao().insert(
                LogEntity(
                    type = "location",
                    data = "${log.address}",
                    secondaryData = "Lat: ${log.lat}, Lon: ${log.lon}",
                    timestamp = log.time
                )
            )

        }
        // Weather
        val loc = locationRepo.getRawLastLocation(context)

        val apiKey = TokenManager.getWeatherApi(context)
        if (apiKey != null && loc != null) {
            val weather = weatherRepo.fetchCurrentWeather(apiKey, loc.lat, loc.lon)
            if (weather != null) {
                val startOfDay = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val lastWeather = db.logDao().getLastLogOfTypeToday("weather", startOfDay)

                val lastDesc = lastWeather?.data?.substringBefore(",")?.trim() // only description
                val newDesc = weather.first.substringBefore(",").trim()        // ignore temp part

                if (lastWeather == null || lastDesc != newDesc) {
                    db.logDao().insert(
                        LogEntity(
                            type = "weather",
                            data = weather.first,          // "Clear sky, 24°C"
                            secondaryData = weather.second, // "☀️"
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    android.util.Log.d("LogSyncManager", "Weather unchanged: $newDesc → skipped")
                }
            }
        }


        // Spotify

        val token = TokenManager.getAccessToken(context)
        var validToken = token

        if (validToken == null) {
            // Try refresh
            validToken = SpotifyAuthManager.refreshAccessToken(context)
        }

        if (validToken != null) {
            val spotifyLogs = spotifyRepo.fetchRecentlyPlayedBlocking(validToken)
            for (log in spotifyLogs) {
                val alreadyExists = db.logDao().exists("spotify", log.time) > 0
                if (!alreadyExists) {
                    db.logDao().insert(
                        LogEntity(
                            type = "spotify",
                            data = log.song,
                            secondaryData = log.artist,
                            timestamp = log.time
                        )
                    )
                }
            }
        }

        // Chess.com
        val chessUsername = TokenManager.getChessUsername(context)
        if (chessUsername != null) {
            val chessLogs = ChessGameRepository().fetchRecentGames(chessUsername)
            for (log in chessLogs) {
                val alreadyExists = db.logDao().exists("chess", log.time) > 0
                if (!alreadyExists) {
                    db.logDao().insert(
                        LogEntity(
                            type = "chess",
                            data = log.primaryText,
                            secondaryData = log.secondaryText,
                            timestamp = log.time
                        )
                    )
                }
            }
        }

    }
}
