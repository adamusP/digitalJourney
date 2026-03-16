package com.example.digitaljourney.ui

// emoji for each type

object EmojiMap {
    val typeToEmoji = mapOf(
        "spotify" to "🎵",
        "photo" to "📸",
        "video" to "🎥",
        "location" to "📍",
        "weather" to "🌤️",
        "call" to "📞",
        "calendar" to "🗓️",
        "chess" to "♟️",
        "text" to "✍️",
        "mood" to "👤"
    )
}

fun emojiFor(type: String): String {
    return EmojiMap.typeToEmoji[type] ?: ""
}


