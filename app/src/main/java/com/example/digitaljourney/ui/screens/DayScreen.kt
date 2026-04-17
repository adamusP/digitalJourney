package com.example.digitaljourney.ui.screens


import com.example.digitaljourney.model.LogEntity

import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.digitaljourney.ui.theme.Purple80
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import coil.request.videoFrameMillis
import com.example.digitaljourney.ui.viewmodel.DayViewModel
import com.example.digitaljourney.ui.emojiFor

import kotlinx.coroutines.delay

import androidx.compose.foundation.clickable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import java.time.LocalDate



// Screen with a list of all the logs in the selected date
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun LogListScreen(
    viewModel: DayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by viewModel.logsForDay.collectAsState()
    val selectedDate by viewModel.selectedDate
    val photos by viewModel.photosForDay
    val videos by viewModel.videosForDay

    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val highlightedTimestamp by viewModel.highlightedLogTimestamp
    val listState = rememberLazyListState()

    val calls by viewModel.callLogsForDay
    var showMap by rememberSaveable { mutableStateOf(false) }

    val allTypes = listOf("spotify", "photo", "video", "location", "weather", "call", "calendar", "chess", "text", "mood")

    var showFilters by rememberSaveable { mutableStateOf(false) }

    val selectedTypes = remember {
        mutableStateListOf(*allTypes.toTypedArray())
    }

    val mediaPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                viewModel.loadPhotosForDate(context, selectedDate)
                viewModel.loadVideosForDate(context, selectedDate)
            }
        }


    val callLogPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                viewModel.loadCallLogsForDate(context, selectedDate)
            }
        }

    // Fetch calls for the date when the screen opens
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        } else {
            viewModel.loadCallLogsForDate(context, selectedDate)
        }
    }



    // Fetch photos for the date when screen opens
    LaunchedEffect(selectedDate) {

        val mediaPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        val mediaGranted = ContextCompat.checkSelfPermission(
            context,
            mediaPermission
        ) == PackageManager.PERMISSION_GRANTED

        if (!mediaGranted) {
            mediaPermissionLauncher.launch(mediaPermission)
        } else {
            viewModel.loadPhotosForDate(context, selectedDate)
            viewModel.loadVideosForDate(context, selectedDate)
        }

        viewModel.loadCallLogsForDate(context, selectedDate)
    }


    // Merge DB logs + photo/video logs + call logs
    val allLogs = remember(logs, photos, videos, calls) {
        val photoEntities = photos.map { log ->
            LogEntity(
                id = -1, // dummy id
                type = "photo",
                data = log.uri.toString(),
                secondaryData = "",
                timestamp = log.timestamp
            )
        }
        val videoEntities = videos.map { log ->
            LogEntity(
                id = -1, // dummy id
                type = "video",
                data = log.uri.toString(),
                secondaryData = "",
                timestamp = log.timestamp
            )
        }
        val callEntities = calls.map {log ->
            LogEntity(
                id = -1,
                type = "call",
                data = log.number.toString() + ", " + log.callType,
                secondaryData = log.duration.toString(),
                timestamp = log.timestamp
            )
        }
        (logs + photoEntities + videoEntities + callEntities).sortedBy { it.timestamp }
    }


    val visibleLogs = remember(allLogs, selectedTypes.size) {
        allLogs.filter { it.type in selectedTypes }
    }

    // highlight from search
    LaunchedEffect(visibleLogs, highlightedTimestamp, showMap) {
        if (!showMap && highlightedTimestamp != null) {
            val index = visibleLogs.indexOfFirst { it.timestamp == highlightedTimestamp }
            if (index != -1) {
                listState.scrollToItem(index)

                delay(3000)

                viewModel.clearHighlightedLog()

            }
        }
    }

    val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            // Header with arrows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.previousDay() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }
                Text(
                    text = "${selectedDate.format(dayFormatter)}, ${selectedDate.format(dateFormatter)}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                IconButton(
                    onClick = { viewModel.nextDay() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Previous month"
                    )
                }
            }

            // Toggle between view modes
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                SegmentedButton(
                    selected = !showMap,
                    onClick = { showMap = false },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    ),
                    icon = {}
                ) {
                    Text("List")
                }

                SegmentedButton(
                    selected = showMap,
                    onClick = { showMap = true },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    ),
                    icon = {}
                ) {
                    Text("Map")
                }
            }

            if (visibleLogs.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text("No activity for this day.")
                }
            } else {
                if (!showMap) {
                    // columns with the logs
                    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
                        items(visibleLogs) { log: LogEntity ->
                            val isHighlighted = log.timestamp == highlightedTimestamp

                            val dt =
                                Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault())
                            val date = dt.format(dateFormatter)
                            val time = dt.format(timeFormatter)

                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isHighlighted) Purple80 else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp)) {
                                // log content depending on type
                                when (log.type) {
                                    "spotify" -> {
                                        Text("$time ${emojiFor(log.type)}")
                                        Text(log.data, fontWeight = FontWeight.SemiBold)
                                        Text(log.secondaryData)
                                    }

                                    "photo" -> {
                                        Text("$time ${emojiFor(log.type)}")
                                        Image(
                                            painter = rememberAsyncImagePainter(Uri.parse(log.data)),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .height(250.dp)
                                        )
                                    }

                                    "video" -> {
                                        Text("$time ${emojiFor(log.type)}")

                                        val painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(context)
                                                .data(Uri.parse(log.data))
                                                .decoderFactory(VideoFrameDecoder.Factory())
                                                .videoFrameMillis(1000)
                                                .build()
                                        )

                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .wrapContentWidth()
                                        ) {
                                            Image(
                                                painter = painter,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.height(250.dp)
                                            )

                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(56.dp)
                                            )
                                        }
                                    }

                                    "location" -> {
                                        Text("$time ${emojiFor(log.type)}")
                                        Text(log.data, fontWeight = FontWeight.SemiBold)
                                    }

                                    "weather" -> {
                                        Text("$time ${log.secondaryData}")
                                        Text(log.data, fontWeight = FontWeight.SemiBold)
                                    }

                                    "call" -> {
                                        Text("$time ${emojiFor(log.type)}")
                                        Text(log.data, fontWeight = FontWeight.SemiBold)
                                        Text(log.secondaryData)
                                    }

                                    "calendar" -> {
                                        Text("$time ${emojiFor(log.type)}")
                                        Text(log.data, fontWeight = FontWeight.SemiBold)
                                        Text(log.secondaryData)
                                    }

                                    "chess" -> {
                                        Text("$time ${emojiFor(log.type)}")
                                        Text(log.data, fontWeight = FontWeight.SemiBold)
                                        Text(log.secondaryData)
                                    }

                                    "text" -> {
                                        Text("$time ${emojiFor(log.type)}")
                                        Text(log.data, fontWeight = FontWeight.SemiBold)
                                    }

                                    "mood" -> {
                                        Text("$time ${emojiFor(log.type)}")
                                        Text(log.data, fontSize = 45.sp)
                                    }

                                    else -> Text(log.data)
                                }
                            }
                        }
                    }
                } else {
                    MapView(allLogs = visibleLogs, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        FloatingActionButton(
            onClick = { showFilters = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp)
                .width(90.dp)
        ) {
            Text("Filters")
        }

        FloatingActionButton(
            onClick = {
                viewModel.refreshSelectedDay(context)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 80.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh"
            )
        }

        // filter pop-up
        if (showFilters) {
            AlertDialog(
                onDismissRequest = { showFilters = false },
                title = { Text("Filter categories") },
                text = {
                    Column {
                        allTypes.forEach { type ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = type in selectedTypes,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (type !in selectedTypes) selectedTypes.add(type)
                                        } else {
                                            selectedTypes.remove(type)
                                        }
                                    }
                                )
                                Text("${emojiFor(type)} $type")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFilters = false }) {
                        Text("Done")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedTypes.clear()
                        selectedTypes.addAll(allTypes)
                    }) {
                        Text("Reset")
                    }
                }
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pickedMillis = datePickerState.selectedDateMillis
                            if (pickedMillis != null) {
                                val pickedDate = Instant.ofEpochMilli(pickedMillis)
                                    .atZone(ZoneId.of("UTC"))
                                    .toLocalDate()

                                viewModel.setDate(pickedDate)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
