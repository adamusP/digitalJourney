package com.example.digitaljourney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.digitaljourney.ui.viewmodel.SearchViewModel
import com.example.digitaljourney.ui.emojiFor
import com.example.digitaljourney.ui.theme.Purple80
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onResultClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val results by viewModel.searchResults

    val allTypes = listOf(
        "spotify", "photo", "video", "location", "weather",
        "call", "calendar", "movie", "chess", "text", "mood"
    )

    var showFilters by remember { mutableStateOf(false) }

    var fromDate by remember { mutableStateOf<LocalDate?>(null) }
    var toDate by remember { mutableStateOf<LocalDate?>(null) }

    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    var newestFirst by remember { mutableStateOf(true) }

    val selectedTypes = remember {
        mutableStateListOf(*allTypes.toTypedArray())
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun runSearch() {
        val input = text.text.trim()
        viewModel.search(
            context = context,
            query = input,
            fromDate = fromDate,
            toDate = toDate,
            selectedTypes = selectedTypes.toSet(),
            newestFirst = newestFirst
        )
    }

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
                .padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { runSearch() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Search")
            }

            OutlinedButton(
                onClick = { showFilters = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Filters")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(results) { log ->
                val dt = Instant.ofEpochMilli(log.timestamp)
                    .atZone(ZoneId.systemDefault())

                val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            onResultClick(log.timestamp)
                        }
                ) {
                    Text(
                        text = highlightQuery(
                            fullText = "${emojiFor(log.type)} ${log.data}",
                            query = text.text
                        )
                    )
                    Text(
                        "Date: ${dt.format(dateTimeFormatter)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // search filter dialog
    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Search filters") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    item {
                        Text("Date range", fontWeight = FontWeight.SemiBold)
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showFromDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(fromDate?.format(dateFormatter) ?: "From")
                            }

                            OutlinedButton(
                                onClick = { showToDatePicker = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(toDate?.format(dateFormatter) ?: "To")
                            }
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = { fromDate = null }) {
                                Text("Clear from")
                            }
                            TextButton(onClick = { toDate = null }) {
                                Text("Clear to")
                            }
                        }
                    }

                    item {
                        Text("Sort order", fontWeight = FontWeight.SemiBold)

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = newestFirst,
                                onClick = { newestFirst = true },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = 0,
                                    count = 2
                                ),
                                icon = {}
                            ) {
                                Text("Newest")
                            }

                            SegmentedButton(
                                selected = !newestFirst,
                                onClick = { newestFirst = false },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = 1,
                                    count = 2
                                ),
                                icon = {}
                            ) {
                                Text("Oldest")
                            }
                        }

                    }

                    item {
                        Text("Types", fontWeight = FontWeight.SemiBold)
                    }

                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            items(allTypes) { type ->
                                val selected = type in selectedTypes

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) Purple80
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            if (selected) {
                                                selectedTypes.remove(type)
                                            } else {
                                                selectedTypes.add(type)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emojiFor(type),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFilters = false
                        runSearch()
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            fromDate = null
                            toDate = null
                            newestFirst = true
                            selectedTypes.clear()
                            selectedTypes.addAll(allTypes)
                        }
                    ) {
                        Text("Reset")
                    }

                    TextButton(
                        onClick = { showFilters = false }
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // date picker for filter selection
    if (showFromDatePicker) {
        val fromPickerState = rememberDatePickerState(
            initialSelectedDateMillis = fromDate
                ?.atStartOfDay(ZoneId.of("UTC"))
                ?.toInstant()
                ?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMillis = fromPickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            fromDate = Instant.ofEpochMilli(pickedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                        }
                        showFromDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFromDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = fromPickerState)
        }
    }

    // date picker for filter selection
    if (showToDatePicker) {
        val toPickerState = rememberDatePickerState(
            initialSelectedDateMillis = toDate
                ?.atStartOfDay(ZoneId.of("UTC"))
                ?.toInstant()
                ?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMillis = toPickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            toDate = Instant.ofEpochMilli(pickedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                        }
                        showToDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showToDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = toPickerState)
        }
    }
}

fun highlightQuery(
    fullText: String,
    query: String
) = buildAnnotatedString {
    if (query.isBlank()) {
        append(fullText)
        return@buildAnnotatedString
    }

    val lowerText = fullText.lowercase()
    val lowerQuery = query.lowercase()

    var startIndex = 0
    var matchIndex = lowerText.indexOf(lowerQuery)

    while (matchIndex >= 0) {
        append(fullText.substring(startIndex, matchIndex))

        withStyle(
            SpanStyle(
                background = Purple80,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(fullText.substring(matchIndex, matchIndex + query.length))
        }

        startIndex = matchIndex + query.length
        matchIndex = lowerText.indexOf(lowerQuery, startIndex)
    }

    append(fullText.substring(startIndex))
}