package com.example.digitaljourney.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitaljourney.model.LogEntity
import com.example.digitaljourney.ui.emojiFor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

private data class LocationPoint(
    val latLng: LatLng,
    val timestamp: Long,
    val label: String
)

private fun parseLatLngFromSecondaryData(text: String): LatLng? {
    val parts = text.split(",").map { it.trim() }
    if (parts.size != 2) return null

    val lat = parts[0].substringAfter("Lat:").trim().toDoubleOrNull() ?: return null
    val lon = parts[1].substringAfter("Lon:").trim().toDoubleOrNull() ?: return null

    return LatLng(lat, lon)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapView(
    allLogs: List<LogEntity>,
    modifier: Modifier = Modifier,
) {
    // Extract location points
    val points = remember(allLogs) {
        allLogs
            .filter { it.type == "location" }
            .mapNotNull { loc ->
                val latLng = parseLatLngFromSecondaryData(loc.secondaryData)
                latLng?.let {
                    LocationPoint(
                        latLng = it,
                        timestamp = loc.timestamp,
                        label = loc.data
                    )
                }
            }
    }

    if (points.isEmpty()) {
        Text(
            "No location points for this day.",
            modifier = modifier.padding(16.dp)
        )
        return
    }

    // Camera start position
    val start = points.first().latLng
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, 13f)
    }

    // Marker selection + bottom sheet state
    var selectedPoint by remember { mutableStateOf<LocationPoint?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // When a marker is selected, show all non-location logs from this location until the next location log appears
    val logsNearSelected = remember(selectedPoint, allLogs) {
        val p = selectedPoint ?: return@remember emptyList()

        val locationLogs = allLogs
            .filter { it.type == "location" }
            .sortedBy { it.timestamp }

        val currentIndex = locationLogs.indexOfFirst { it.timestamp == p.timestamp }
        if (currentIndex == -1) return@remember emptyList()

        val startTime = locationLogs[currentIndex].timestamp
        val endTime = locationLogs.getOrNull(currentIndex + 1)?.timestamp ?: Long.MAX_VALUE

        allLogs
            .filter { it.type != "location" }
            .filter { it.timestamp in startTime until endTime }
            .sortedBy { it.timestamp }
            .groupBy { it.type }
            .flatMap { (_, logsOfType) -> logsOfType.take(3) }
            .sortedBy { it.timestamp }
    }

    // Map
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        )
    ) {
        points.forEach { p ->
            Marker(
                state = MarkerState(position = p.latLng),
                title = p.label,
                onClick = {
                    selectedPoint = p
                    false
                }
            )
        }
    }

    // Bottom sheet with activities in that location
    if (selectedPoint != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedPoint = null },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(selectedPoint!!.label)

                LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                    items(logsNearSelected) { log ->
                        Text(
                            text = if (log.type == "photo") {
                                "${emojiFor(log.type)} Photo"
                            }
                            else if (log.type == "video") {
                                "${emojiFor(log.type)} Video"
                            }
                            else {
                                "${emojiFor(log.type)} ${log.type}: ${log.data}"
                            },
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// Small helper to chain nullables without extra noise
private inline fun <T> T?.orElse(block: () -> T?): T? = this ?: block()