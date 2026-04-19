package com.example.digitaljourney.data.managers

import android.content.Context
import android.util.Log
import com.example.digitaljourney.data.repositories.CalendarRepository
import com.example.digitaljourney.data.repositories.ChessGameRepository
import com.example.digitaljourney.data.repositories.MovieRepository
import com.example.digitaljourney.data.repositories.LocationRepository
import com.example.digitaljourney.data.repositories.PhotosRepository
import com.example.digitaljourney.data.repositories.SpotifyFetchResult
import com.example.digitaljourney.data.repositories.SpotifyRepository
import com.example.digitaljourney.data.repositories.WeatherRepository
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
    private val locationRepo: LocationRepository,
    private val weatherRepo: WeatherRepository,
    private val calendarRepo: CalendarRepository
) {
    private val db = AppDatabase.getInstance(context)

    // calls all the repositories for the current data
    suspend fun syncNow() = withContext(Dispatchers.IO) {
        try {

            // Location
            try {
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
            } catch (e: Exception) {
                Log.e("LogSyncManager", "Location sync failed", e)
            }

            // Weather
            try {
                val loc = locationRepo.getRawLastLocation(context)
                val apiKey = TokenManager.getWeatherApi()
                if (apiKey != null && loc != null) {
                    val weather = weatherRepo.fetchCurrentWeather(apiKey, loc.lat, loc.lon)
                    if (weather != null) {
                        val startOfDay = LocalDate.now()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                        val lastWeather = db.logDao().getLastLogOfTypeToday("weather", startOfDay)

                        val lastDesc = lastWeather?.data?.substringBefore(",")?.trim()
                        val newDesc = weather.first.substringBefore(",").trim()

                        if (lastWeather == null || lastDesc != newDesc) {
                            db.logDao().insert(
                                LogEntity(
                                    type = "weather",
                                    data = weather.first,
                                    secondaryData = weather.second,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } else {
                            Log.d("LogSyncManager", "Weather unchanged: $newDesc , skipped")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LogSyncManager", "Weather sync failed", e)
            }

            // Spotify
            try {
                val savedToken = TokenManager.getSpotifyAccessToken(context)

                var spotifyLogs: List<com.example.digitaljourney.model.LogEntry.SpotifyLog> = emptyList()

                if (savedToken != null) {
                    when (val result = spotifyRepo.fetchRecentlyPlayedBlocking(savedToken)) {
                        is SpotifyFetchResult.Success -> {
                            spotifyLogs = result.logs
                        }
                        is SpotifyFetchResult.Unauthorized -> {

                            val refreshedToken = SpotifyAuthManager.refreshAccessToken(context)

                            if (refreshedToken != null) {

                                when (val retryResult = spotifyRepo.fetchRecentlyPlayedBlocking(refreshedToken)) {
                                    is SpotifyFetchResult.Success -> {
                                        spotifyLogs = retryResult.logs
                                    }
                                    is SpotifyFetchResult.Unauthorized -> {
                                        Log.e("LogSyncManager", "Spotify still returned 401 even after refresh")
                                    }
                                    is SpotifyFetchResult.Error -> {
                                        Log.e("LogSyncManager", "Spotify request failed after refresh")
                                    }
                                }
                            } else {
                                Log.e("LogSyncManager", "Spotify refresh failed")
                            }
                        }
                        is SpotifyFetchResult.Error -> {
                            Log.e("LogSyncManager", "Spotify request failed with non-auth error")
                        }
                    }
                } else {
                    Log.d("LogSyncManager", "No Spotify access token found, trying refresh directly")

                    val refreshedToken = SpotifyAuthManager.refreshAccessToken(context)
                    if (refreshedToken != null) {

                        when (val retryResult = spotifyRepo.fetchRecentlyPlayedBlocking(refreshedToken)) {
                            is SpotifyFetchResult.Success -> {
                                spotifyLogs = retryResult.logs
                            }
                            is SpotifyFetchResult.Unauthorized -> {
                                Log.e("LogSyncManager", "Spotify still unauthorized after refresh")
                            }
                            is SpotifyFetchResult.Error -> {
                                Log.e("LogSyncManager", "Spotify request failed after refresh")
                            }
                        }
                    } else {
                        Log.e("LogSyncManager", "Spotify refresh failed and no saved access token exists")
                    }
                }

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
            } catch (e: Exception) {
                Log.e("LogSyncManager", "Spotify sync failed", e)
            }

            // Calendar
            try {
                val freshToken = GoogleCalendarAuth.tryRefreshAccessTokenSilently(context)
                if (!freshToken.isNullOrBlank()) {
                    TokenManager.saveGoogleTokens(context, freshToken, null)
                    CalendarRepository.syncCalendarLogs(context)
                }
            } catch (e: Exception) {
                Log.e("LogSyncManager", "Calendar sync failed", e)
            }

            // Letterboxd
            try {
                val letterboxdUsername = TokenManager.getLetrUsername(context)
                if (letterboxdUsername != null) {
                    val movieLogs = MovieRepository().fetchRecentMovies(letterboxdUsername)
                    for (log in movieLogs) {
                        val alreadyExists = db.logDao().exists("movie", log.time) > 0
                        if (!alreadyExists) {
                            db.logDao().insert(
                                LogEntity(
                                    type = "movie",
                                    data = log.primaryText,
                                    secondaryData = log.secondaryText,
                                    timestamp = log.time
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LogSyncManager", "Letterboxd sync failed", e)
            }

            // Chess
            try {
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
                } else {}
            } catch (e: Exception) {
                Log.e("LogSyncManager", "Chess sync failed", e)
            }

        } catch (e: Exception) {
            Log.e("LogSyncManager", "syncNow crashed", e)
        }
    }
}
