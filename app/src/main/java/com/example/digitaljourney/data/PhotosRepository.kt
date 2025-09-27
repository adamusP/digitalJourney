package com.example.digitaljourney.data

import android.net.Uri
import com.example.digitaljourney.model.LogEntry
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.time.LocalDate
import java.time.ZoneId

interface PhotosRepository {
    fun fetchTodayPhotos(context: android.content.Context): List<LogEntry.PhotoLog>
    fun fetchPhotosForDate(context: Context, date: LocalDate): List<LogEntry.PhotoLog>
}



class PhotosRepositoryImpl : PhotosRepository {

    override fun fetchTodayPhotos(context: Context): List<LogEntry.PhotoLog> {
        val todayLogs = mutableListOf<LogEntry.PhotoLog>()

        val collection: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val startOfDaySecs = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toEpochSecond()

        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
        val selectionArgs = arrayOf(startOfDaySecs.toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketCol) ?: ""

                // only include Camera and Screenshots
                if (bucketName.contains("Camera", ignoreCase = true) ||
                    bucketName.contains("Screenshots", ignoreCase = true)) {

                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)

                    // DATE_ADDED is in seconds, convert to millis
                    val dateAddedSecs = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                    val timestampMillis = dateAddedSecs * 1000

                    todayLogs.add(LogEntry.PhotoLog(uri, timestampMillis))
                }
            }
        }

        return todayLogs
    }

    override fun fetchPhotosForDate(context: Context, date: LocalDate): List<LogEntry.PhotoLog> {
        val logs = mutableListOf<LogEntry.PhotoLog>()

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

        val selection = "${MediaStore.Images.Media.DATE_ADDED} BETWEEN ? AND ?"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val timestamp = cursor.getLong(dateCol) * 1000 // seconds → millis

                logs.add(LogEntry.PhotoLog(uri, timestamp))
            }
        }

        return logs
    }
}