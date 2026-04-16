package com.example.digitaljourney.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import androidx.compose.runtime.setValue
import com.example.digitaljourney.ui.viewmodel.SearchViewModel
import com.example.digitaljourney.ui.emojiFor

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onResultClick: (Long) -> Unit,
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
                            onResultClick(log.timestamp)
                        }
                ) {
                    Text("${emojiFor(log.type)} ${log.data}", fontWeight = FontWeight.Bold)
                    Text("Date: $dt", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}