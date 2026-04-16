package com.example.digitaljourney.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.example.digitaljourney.ui.viewmodel.LogViewModel


// Screen with the buttons to log moods and a text field to log text
@Composable
fun LogScreen(
    viewModel: LogViewModel,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var showJournal by rememberSaveable { mutableStateOf(false) }

    // Emoji data with description
    val emojiList = listOf(
        EmojiItem("😄", "Joyful"),
        EmojiItem("😂", "Laughing"),
        EmojiItem("😊", "Happy"),
        EmojiItem("🙂", "Content"),
        EmojiItem("😌", "Peaceful"),
        EmojiItem("😎", "Confident"),
        EmojiItem("😍", "Loving"),
        EmojiItem("🥰", "Affectionate"),
        EmojiItem("🤩", "Excited"),
        EmojiItem("🤔", "Thoughtful"),
        EmojiItem("🥱", "Sleepy"),
        EmojiItem("😴", "Tired"),
        EmojiItem("😮", "Surprised"),
        EmojiItem("😐", "Neutral"),
        EmojiItem("😑", "Indifferent"),
        EmojiItem("😕", "Confused"),
        EmojiItem("🙁", "Sad"),
        EmojiItem("😢", "Very sad"),
        EmojiItem("😞", "Disappointed"),
        EmojiItem("😟", "Worried"),
        EmojiItem("😰", "Anxious"),
        EmojiItem("😣", "Stressed"),
        EmojiItem("😒", "Annoyed"),
        EmojiItem("😡", "Angry")
    )

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = !showJournal,
                    onClick = { showJournal = false },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    ),
                    icon = {}
                ) {
                    Text("Mood")
                }

                SegmentedButton(
                    selected = showJournal,
                    onClick = { showJournal = true },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    ),
                    icon = {}
                ) {
                    Text("Journal")
                }
            }

            if (!showJournal) {

                Text(
                    "How are you feeling?",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    itemsIndexed(emojiList) { index, emoji ->
                        if (index == 11) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            )
                        }

                        EmojiButton(emoji) {
                            viewModel.logMood(
                                emoji = emoji.emoji,
                                description = emoji.description
                            )
                        }
                    }
                }
            } else {
                Text(
                    "What's on your mind?",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                )

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Write something...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(280.dp)
                )

                Button(
                    onClick = {
                        viewModel.logText(text.text) {
                            text = TextFieldValue("")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log")
                }
            }


        }
    }
}

// helper class
data class EmojiItem(val emoji: String, val description: String)
@Composable
fun EmojiButton(emojiItem: EmojiItem, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(6.dp)
            .size(64.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(emojiItem.emoji, fontSize = 28.sp)
    }
}
