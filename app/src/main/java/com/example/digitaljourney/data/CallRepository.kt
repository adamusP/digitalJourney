package com.example.digitaljourney.data

import android.content.Context
import android.provider.CallLog
import android.util.Log
import com.example.digitaljourney.model.LogEntry
import java.time.LocalDate
import java.time.ZoneId

import android.net.Uri
import android.provider.ContactsContract
import okhttp3.internal.format


interface CallRepository {
    fun fetchCallsForDate(context: Context, date: LocalDate): List<LogEntry.CallLog>
    fun getContactName(context: Context, phoneNumber: String?): String?
}

class CallRepositoryImpl : CallRepository {

    // returns a list of all the calls logged during the date
    override fun fetchCallsForDate(context: Context, date: LocalDate): List<LogEntry.CallLog> {
        val logs = mutableListOf<LogEntry.CallLog>()

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val selection = "${CallLog.Calls.DATE} BETWEEN ? AND ?"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
        val sortOrder = "${CallLog.Calls.DATE} ASC"

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberCol)
                val type = cursor.getInt(typeCol)
                val timestamp = cursor.getLong(dateCol)
                val duration = cursor.getLong(durationCol)

                logs.add(
                    LogEntry.CallLog(
                        number = getContactName(context, number) ?: number,
                        callType = typeToString(type),
                        time = timestamp,
                        duration = formatSeconds(duration)
                    )
                )
            }
        }


        return logs
    }

    // gets the name of the contact based on the phone number
    override fun getContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber == null) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    // returns the string of the type of call
    fun typeToString(type: Int): String {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> "incoming"
            CallLog.Calls.OUTGOING_TYPE -> "outgoing"
            CallLog.Calls.MISSED_TYPE -> "missed"
            CallLog.Calls.REJECTED_TYPE -> "rejected"
            CallLog.Calls.BLOCKED_TYPE -> "blocked"
            else -> "other"
        }
    }

    // formats seconds into a string
    fun formatSeconds(seconds: Long): String {
        if (seconds < 60) {
            return "${seconds}s"
        }

        val minutes = seconds / 60
        val secs = seconds % 60

        if (seconds < 3600) {
            return String.format("%d:%02d min", minutes, secs)
        }

        val hours = seconds / 3600
        val remaining = seconds % 3600
        val mins = remaining / 60
        val sec = remaining % 60

        return String.format("%d:%02d:%02d h", hours, mins, sec)
    }

}
