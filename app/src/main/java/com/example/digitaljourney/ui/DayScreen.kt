package com.example.digitaljourney.ui

import android.net.Uri
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
import coil.compose.rememberAsyncImagePainter
import com.example.digitaljourney.data.LocationRepositoryImpl
import com.example.digitaljourney.data.LogSyncManager
import com.example.digitaljourney.data.PhotosRepositoryImpl
import com.example.digitaljourney.data.SpotifyRepositoryImpl
import com.example.digitaljourney.data.WeatherRepository
import com.example.digitaljourney.model.LogEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import android.content.pm.PackageManager


// Screen with a list of all the logs in the selected date
@Composable
public fun LogListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by viewModel.logsForDay.collectAsState()
    val selectedDate by viewModel.selectedDate
    val photos by viewModel.photosForDay
    val calls by viewModel.callLogsForDay


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

        val sync = LogSyncManager(
            context,
            SpotifyRepositoryImpl(),
            PhotosRepositoryImpl(),
            LocationRepositoryImpl(),
            WeatherRepository()
        )
        sync.syncNow()

        viewModel.loadPhotosForDate(context, selectedDate)
        viewModel.loadCallLogsForDate(context, selectedDate)
    }


    // Merge DB logs + photo logs + call logs
    val allLogs = remember(logs, photos) {
        val photoEntities = photos.map { log ->
            LogEntity(
                id = -1, // dummy id
                type = "photo",
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
        (logs + photoEntities + callEntities).sortedBy { it.timestamp }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(modifier = modifier.fillMaxWidth()
        // attempt to have sweeping effect
//        .pointerInput(Unit) {
//        detectHorizontalDragGestures { _, dragAmount ->
//            val threshold = 100f // swipe distance in pixels before it counts
//            if (dragAmount > threshold) {
//                viewModel.previousDay()
//            } else if (dragAmount < -threshold) {
//                viewModel.nextDay()
//            }
//        }
//    }
    ) {
        // Header with arrows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
            Text(selectedDate.format(dateFormatter), fontWeight = FontWeight.Bold)
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

        if (allLogs.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("No activity for this day.")
            }
        } else {
            // columns with the logs
            LazyColumn(modifier = modifier.fillMaxWidth()) {
                items(allLogs) { log: LogEntity ->
                    val dt = Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault())
                    val date = dt.format(dateFormatter)
                    val time = dt.format(timeFormatter)

                    Column(modifier = Modifier.padding(8.dp)) {
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
                                //Text(log.secondaryData)
                            }

                            else -> Text(log.data)
                        }
                    }
                }
            }
        }
    }
}
