package com.example.digitaljourney.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

import java.time.Instant
import java.time.ZoneId

import androidx.compose.foundation.clickable

// Screen that shows text input and button for searching all logs
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onResultClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current
    val results by viewModel.searchResults


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Search...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                val input = text.text.trim()
                if (input.isNotEmpty()) {
                    viewModel.search(context, input)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // List of results
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(results) { log ->
                val dt = Instant.ofEpochMilli(log.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.goToLogDay(log.timestamp)   // on clicking the result, user is sent to the days logs
                            onResultClick()
                        }
                ) {
                    Text(emojiFor(log.type) + log.data, fontWeight = FontWeight.Bold)
                    Text("Date: $dt", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
