package com.example.digitaljourney.data.repositories

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class WeatherRepository {

    private val client = OkHttpClient()

    // Fetches current weather using OpenWeatherMap API, returns Pair(description, emoji) or null if failed
    fun fetchCurrentWeather(apiKey: String, lat: Double, lon: Double): Pair<String, String>? {
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"

        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = response.body?.string() ?: return null
                val obj = JSONObject(json)

                val weatherDesc = obj.getJSONArray("weather").getJSONObject(0).getString("description")
                val temp = obj.getJSONObject("main").getDouble("temp").toInt()

                val sys = obj.getJSONObject("sys")
                val sunrise = sys.getLong("sunrise")
                val sunset = sys.getLong("sunset")
                val now = System.currentTimeMillis() / 1000

                val isDay = now in sunrise..sunset

                // map for weather emojis
                val emoji = when {
                    weatherDesc.contains("clear", true) && isDay -> "☀️"
                    weatherDesc.contains("clear", true) && !isDay -> "🌙"
                    weatherDesc.contains("cloud", true) && isDay -> "🌤️"
                    weatherDesc.contains("cloud", true) && !isDay -> "☁️"
                    weatherDesc.contains("rain", true) -> "🌧️"
                    weatherDesc.contains("drizzle", true) -> "🌧️"
                    weatherDesc.contains("snow", true) -> "❄️"
                    weatherDesc.contains("storm", true) -> "⛈️"
                    else -> "🌡️"
                }

                val description = "${weatherDesc.replaceFirstChar { it.uppercase() }}, $temp°C"
                Pair(description, emoji)
            }
        } catch (e: IOException) {
            android.util.Log.e("WeatherRepository", "Weather fetch failed", e)
            null
        }
    }
}
