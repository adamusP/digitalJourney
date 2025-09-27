package com.example.digitaljourney.data

import android.app.Activity
import android.content.Context
import android.location.Geocoder
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.example.digitaljourney.model.LogEntry
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import java.util.Locale
import android.location.Location
import kotlin.math.*
import java.time.LocalDate
import java.time.ZoneId
import com.example.digitaljourney.model.AppDatabase

interface LocationRepository {
    fun fetchLastKnownLocation(
        activity: Activity,
        onResult: (LogEntry.LocationLog?) -> Unit
    )

    suspend fun fetchLastKnownLocationBlocking(
        context: Context
    ): LogEntry.LocationLog?

    // always return last location, no distance filtering
    suspend fun getRawLastLocation(
        context: Context
    ): LogEntry.LocationLog?
}

class LocationRepositoryImpl : LocationRepository {

    override fun fetchLastKnownLocation(
        activity: Activity,
        onResult: (LogEntry.LocationLog?) -> Unit
    ) {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            onResult(null)
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(activity)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(activity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addressText = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else null

                onResult(
                    LogEntry.LocationLog(
                        lat = location.latitude,
                        lon = location.longitude,
                        address = addressText,
                        time = System.currentTimeMillis()
                    )
                )
            } else {
                onResult(null)
            }
        }
    }

    override suspend fun fetchLastKnownLocationBlocking(
        context: Context
    ): LogEntry.LocationLog? {
        return try {
            // Check permission
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                android.util.Log.w("LocationRepository", "Location permission not granted")
                return null
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val location = Tasks.await(fusedClient.lastLocation)

            if (location != null) {

                val db = AppDatabase.getInstance(context)

                // start of today in millis
                val startOfDay = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val lastLog = db.logDao().getLastLogOfTypeToday("location", startOfDay)

                if (lastLog != null) {
                    // Compare distance with last stored location
                    val parts = lastLog.secondaryData.split(",")
                    val lat = parts[0].substringAfter("Lat:").trim().toDoubleOrNull()
                    val lon = parts[1].substringAfter("Lon:").trim().toDoubleOrNull()

                    if (lat != null && lon != null) {
                        val dist = distanceInMeters(lat, lon, location.latitude, location.longitude)
                        if (dist < 100.0) {
                            android.util.Log.d("LocationRepository", "Skipping log, only $dist m moved")
                            return null
                        }
                    }
                }

                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addressText = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else null

                LogEntry.LocationLog(
                    lat = location.latitude,
                    lon = location.longitude,
                    address = addressText,
                    time = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationRepository", "Error fetching location", e)
            null
        }
    }

    override suspend fun getRawLastLocation(context: Context): LogEntry.LocationLog? {
        return try {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                android.util.Log.w("LocationRepository", "Location permission not granted")
                return null
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val location = Tasks.await(fusedClient.lastLocation)

            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addressText = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else null

                LogEntry.LocationLog(
                    lat = location.latitude,
                    lon = location.longitude,
                    address = addressText,
                    time = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationRepository", "Error fetching raw location", e)
            null
        }
    }

}

// Haversine distance between two lat/lon pairs in meters.
private fun distanceInMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}
