package com.example.digitaljourney.data.repositories

import android.util.Log
import android.util.Xml
import com.example.digitaljourney.model.LogEntry
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.Locale

class MovieRepository {
    private val client = OkHttpClient()

    fun fetchRecentMovies(username: String): List<LogEntry.MovieLog> {
        val result = mutableListOf<LogEntry.MovieLog>()

        try {
            val normalizedUsername = username.trim().lowercase(Locale.getDefault())
            if (normalizedUsername.isBlank()) return emptyList()

            val url = "https://letterboxd.com/$normalizedUsername/rss/"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return emptyList()

            val xml = response.body?.string() ?: return emptyList()

            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType

            var insideItem = false
            var filmTitle = ""
            var filmYear = ""
            var memberRating = ""
            var watchedDate = ""
            var pubDate = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name

                        when (tagName) {
                            "item" -> {
                                insideItem = true
                                filmTitle = ""
                                filmYear = ""
                                memberRating = ""
                                watchedDate = ""
                                pubDate = ""
                            }

                            "filmTitle" -> {
                                if (insideItem) filmTitle = parser.nextText().trim()
                            }

                            "filmYear" -> {
                                if (insideItem) filmYear = parser.nextText().trim()
                            }

                            "memberRating" -> {
                                if (insideItem) memberRating = parser.nextText().trim()
                            }

                            "watchedDate" -> {
                                if (insideItem) watchedDate = parser.nextText().trim()
                            }

                            "pubDate" -> {
                                if (insideItem) pubDate = parser.nextText().trim()
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" && insideItem) {
                            val primary = buildPrimaryText(filmTitle, filmYear)
                            val secondary = buildStarText(memberRating)
                            val time = parseLetterboxdDate(watchedDate, pubDate)

                            result.add(
                                LogEntry.MovieLog(
                                    primaryText = primary,
                                    secondaryText = secondary,
                                    time = time
                                )
                            )

                            insideItem = false
                        }
                    }
                }

                eventType = parser.next()
            }

        } catch (e: Exception) {
            Log.e("LetterboxdRepository", "Failed to fetch Letterboxd RSS", e)
        }

        return result
    }

    private fun buildPrimaryText(title: String, year: String): String {
        return when {
            title.isNotBlank() && year.isNotBlank() -> "$title ($year)"
            title.isNotBlank() -> title
            else -> "Movie"
        }
    }

    private fun buildStarText(rating: String): String {
        return when (rating) {
            "0.5" -> "½"
            "1.0" -> "★"
            "1.5" -> "★½"
            "2.0" -> "★★"
            "2.5" -> "★★½"
            "3.0" -> "★★★"
            "3.5" -> "★★★½"
            "4.0" -> "★★★★"
            "4.5" -> "★★★★½"
            "5.0" -> "★★★★★"
            else -> ""
        }
    }

    private fun parseLetterboxdDate(watchedDate: String, pubDate: String): Long {
        return try {
            if (pubDate.isNotBlank()) {
                val formatter = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                return formatter.parse(pubDate)?.time ?: System.currentTimeMillis()
            }

            if (watchedDate.isNotBlank()) {
                val parts = watchedDate.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt() - 1
                    val day = parts[2].toInt()

                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(year, month, day, 12, 0, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    return calendar.timeInMillis
                }
            }

            System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}