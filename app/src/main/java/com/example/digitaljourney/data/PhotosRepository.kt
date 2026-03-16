package com.example.digitaljourney.data

import com.example.digitaljourney.model.LogEntry
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import java.time.LocalDate
import java.time.ZoneId

interface PhotosRepository {
    fun fetchPhotosForDate(context: Context, date: LocalDate): List<LogEntry.PhotoLog>
    fun fetchVideosForDate(context: Context, date: LocalDate): List<LogEntry.VideoLog>
}

class PhotosRepositoryImpl : PhotosRepository {

    // returns a list of all the logs of photos for the date
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
                val timestamp = cursor.getLong(dateCol) * 1000 // seconds to millis

                logs.add(LogEntry.PhotoLog(uri, timestamp))
            }
        }

        return logs
    }

    // returns a list of all the logs of videos for the date
    override fun fetchVideosForDate(context: Context, date: LocalDate): List<LogEntry.VideoLog> {
        val logs = mutableListOf<LogEntry.VideoLog>()

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED
        )

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

        val selection = "${MediaStore.Video.Media.DATE_ADDED} BETWEEN ? AND ?"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val timestamp = cursor.getLong(dateCol) * 1000 // seconds to millis

                logs.add(LogEntry.VideoLog(uri, timestamp))
            }
        }

        return logs
    }
}