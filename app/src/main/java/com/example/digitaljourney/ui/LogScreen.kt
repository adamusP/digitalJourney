package com.example.digitaljourney.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.digitaljourney.model.AppDatabase
import com.example.digitaljourney.model.LogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.font.FontWeight


@Composable
fun LogScreen(
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Sample emoji data with description
    val emojiList = listOf(
        EmojiItem("😄", "Joyful"),
        EmojiItem("😂", "Laughing"),
        EmojiItem("😊", "Happy"),
        EmojiItem("😌", "Satisfied"),
        EmojiItem("😑", "Indifferent"),
        EmojiItem("🙁", "Sad"),
        EmojiItem("😢", "Very sad"),
        EmojiItem("😒", "Annoyed"),
        EmojiItem("😎", "Cool"),
        EmojiItem("😍", "Love"),
        EmojiItem("😮", "Surprised"),
        EmojiItem("😡", "Angry")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Log your mood", fontSize = 35.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))

        // Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            items(emojiList) { emoji ->
                EmojiButton(emoji) {
                    // Log emoji click
                    val input = emoji.emoji
                    val description = emoji.description
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getInstance(context)
                        db.logDao().insert(
                            LogEntity(
                                type = "mood",
                                data = input,
                                secondaryData = description,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }

        Text("Log your thoughs", fontSize = 35.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))

        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Write something...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(200.dp)
        )

        Button(
            onClick = {
                val input = text.text.trim()
                if (input.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getInstance(context)
                        db.logDao().insert(
                            LogEntity(
                                type = "text",
                                data = input,
                                secondaryData = "",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    text = TextFieldValue("") // clear field
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

data class EmojiItem(val emoji: String, val description: String)
@Composable
fun EmojiButton(emojiItem: EmojiItem, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .height(48.dp),
        content = {
            Text(emojiItem.emoji, fontSize = 24.sp)
        }
    )
}
