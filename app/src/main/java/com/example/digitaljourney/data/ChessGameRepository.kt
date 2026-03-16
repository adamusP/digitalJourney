package com.example.digitaljourney.data

import android.util.Log
import com.example.digitaljourney.model.LogEntry
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

class ChessGameRepository {
    private val client = OkHttpClient()

    // returns a list of all the games played in the last available date range (a month)
    fun fetchRecentGames(username: String): List<LogEntry.ChessLog> {
        val result = mutableListOf<LogEntry.ChessLog>()
        try {
            val normalizedUsername = username.lowercase(Locale.getDefault())

            // Get archive list
            val archivesUrl = "https://api.chess.com/pub/player/$normalizedUsername/games/archives"
            val archivesReq = Request.Builder().url(archivesUrl).build()
            val archivesResp = client.newCall(archivesReq).execute()
            if (!archivesResp.isSuccessful) return emptyList()

            val archivesJson = JSONObject(archivesResp.body?.string() ?: return emptyList())
            val archives = archivesJson.getJSONArray("archives")
            if (archives.length() == 0) return emptyList()

            // Fetch last month’s games
            val lastArchive = archives.getString(archives.length() - 1)
            val gamesReq = Request.Builder().url(lastArchive).build()
            val gamesResp = client.newCall(gamesReq).execute()
            if (!gamesResp.isSuccessful) return emptyList()

            val gamesJson = JSONObject(gamesResp.body?.string() ?: return emptyList())
            val games = gamesJson.getJSONArray("games")

            // loop all games found
            for (i in 0 until games.length()) {
                val game = games.getJSONObject(i)
                val endTime = game.optLong("end_time", 0) * 1000

                val white = game.getJSONObject("white").getString("username").lowercase(Locale.getDefault())
                val black = game.getJSONObject("black").getString("username").lowercase(Locale.getDefault())
                val timeClass = game.optString("time_class", "unknown") // blitz, rapid, bullet
                val resultWhite = game.getJSONObject("white").optString("result", "")
                val resultBlack = game.getJSONObject("black").optString("result", "")

                // Determine white or black
                val isWhite = (white == normalizedUsername)
                val myResult = if (isWhite) resultWhite else resultBlack

                // Primary text
                val primary = "${timeClass.replaceFirstChar { it.uppercase() }} game with " +
                        if (isWhite) "white pieces" else "black pieces"

                // Secondary text
                val secondary = when (myResult) {
                    "win" -> "Won"
                    "checkmated" -> "Lost by checkmate"
                    "timeout" -> "Lost by timeout"
                    "resigned" -> "Lost by resignation"
                    "agreed" -> "Draw by agreement"
                    "stalemate" -> "Draw by stalemate"
                    "repetition" -> "Draw by repetition"
                    "timevsinsufficient" -> "Draw (timeout vs insufficient material)"
                    else -> myResult.replaceFirstChar { it.uppercase() }
                }

                result.add(
                    LogEntry.ChessLog(
                        primaryText = primary,
                        secondaryText = secondary,
                        time = endTime
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("ChessGameRepository", "Failed to fetch games", e)
        }
        return result
    }
}
