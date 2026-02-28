package com.example.digitaljourney.ui
import com.example.digitaljourney.data.PhotosRepositoryImpl
import com.example.digitaljourney.data.CallRepositoryImpl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.digitaljourney.data.CallRepository
import com.example.digitaljourney.data.PhotosRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Screen with the day grid for the selected month, with filters
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    viewModel: MainViewModel,
    onDaySelected: (LocalDate) -> Unit
) {
    val selectedMonth by viewModel.selectedMonth
    val selectedFilter by viewModel.selectedFilter
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    var expanded by remember { mutableStateOf(false) }
    val filterOptions = listOf("Off", "All", "spotify", "photo", "location", "weather", "call", "chess", "text", "mood")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousMonth() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(selectedMonth.format(dateFormatter), fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.nextMonth() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = selectedFilter,
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter") },
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded)
                                Icons.Filled.KeyboardArrowUp
                            else
                                Icons.Filled.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded } // ensures tap anywhere works
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
            ) {
                filterOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            viewModel.selectedFilter.value = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Grid of days
        val daysInMonth = selectedMonth.lengthOfMonth()
        val firstDayOfWeek = selectedMonth.withDayOfMonth(1).dayOfWeek.value - 1
        val totalCells = daysInMonth + firstDayOfWeek

        val logs by viewModel.getLogsForMonth(selectedMonth).collectAsState(initial = emptyList())
        val logsByDay = remember(logs) {
            logs.groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(ZoneOffset.UTC).toLocalDate()
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            for (weekStart in 0 until totalCells step 7) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayIndex in 0..6) {
                        val cellIndex = weekStart + dayIndex
                        if (cellIndex < firstDayOfWeek || cellIndex >= daysInMonth + firstDayOfWeek) {
                            Box(modifier = Modifier.size(40.dp))
                        } else {
                            val day = cellIndex - firstDayOfWeek + 1
                            val dayDate = selectedMonth.withDayOfMonth(day)
                            val entries = logsByDay[dayDate].orEmpty()

                            // gets the entries which we need to count in order to show filter
                            val filteredEntries = when (selectedFilter) {
                                "Off" -> emptyList()
                                "All" -> entries +
                                        PhotosRepositoryImpl().fetchPhotosForDate(LocalContext.current, dayDate) +
                                        CallRepositoryImpl().fetchCallsForDate(LocalContext.current, dayDate)
                                "photo" -> PhotosRepositoryImpl().fetchPhotosForDate(LocalContext.current, dayDate)
                                "call" -> CallRepositoryImpl().fetchCallsForDate(LocalContext.current, dayDate)
                                else -> entries.filter { it.type == selectedFilter }
                            }

                            val count = filteredEntries.size
                            val intensity = count.coerceIn(0, 10) / 5f
                            val color = if (selectedFilter == "Off") {
                                ButtonDefaults.buttonColors()
                            } else {
                                val baseColor = Color(0xFF81C784)
                                val blended = baseColor.copy(alpha = 0.3f + 0.7f * intensity)   // color fade
                                ButtonDefaults.buttonColors(containerColor = blended)
                            }

                            Button(
                                onClick = {
                                    val clickedDate = selectedMonth.withDayOfMonth(day)
                                    viewModel.setDate(clickedDate)
                                    onDaySelected(clickedDate)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(horizontal = 2.dp),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(8.dp),
                                colors = color
                            ) {
                                Text(day.toString())
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(128.dp))

        // Today button
        Button(
            onClick = {
                val todayDate = LocalDate.now()
                viewModel.setDate(todayDate)
                onDaySelected(todayDate)
            },
            modifier = Modifier
                .padding(16.dp)
                .height(48.dp)
                .width(120.dp)
        ) {
            Text("Today", fontWeight = FontWeight.Bold)
        }
    }
}