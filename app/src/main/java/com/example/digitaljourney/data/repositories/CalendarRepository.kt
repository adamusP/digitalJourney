package com.example.digitaljourney.data.repositories

import android.content.Context
import com.example.digitaljourney.data.GoogleCalendarApi
import com.example.digitaljourney.data.managers.GoogleCalendarAuth
import com.example.digitaljourney.data.managers.TokenManager
import com.example.digitaljourney.model.AppDatabase
import com.example.digitaljourney.model.LogDao
import com.example.digitaljourney.model.LogEntity
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId


object CalendarRepository {

    suspend fun syncCalendarLogs(context: Context) {
        var accessToken = TokenManager.getGoogleAccessToken(context)

        if (accessToken.isNullOrBlank()) {
            accessToken = GoogleCalendarAuth.tryRefreshAccessTokenSilently(context)
            if (accessToken.isNullOrBlank()) return
            TokenManager.saveGoogleTokens(context, accessToken, null)
        }

        val api = provideGoogleCalendarApi()
        val dao = AppDatabase.getInstance(context).logDao()

        try {
            fullSync(context, api, dao, accessToken)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                val freshToken = GoogleCalendarAuth.tryRefreshAccessTokenSilently(context)
                if (freshToken.isNullOrBlank()) throw e

                TokenManager.saveGoogleTokens(context, freshToken, null)
                fullSync(context, api, dao, freshToken)
            } else {
                throw e
            }
        }
    }

    private suspend fun performCalendarSync(
        context: Context,
        api: GoogleCalendarApi,
        dao: LogDao,
        accessToken: String
    ) {
        val syncToken = TokenManager.getGoogleCalendarSyncToken(context)

        if (syncToken.isNullOrBlank()) {
            fullSync(context, api, dao, accessToken)
        } else {
            incrementalSync(context, api, dao, accessToken, syncToken)
        }
    }

    private fun provideGoogleCalendarApi(): GoogleCalendarApi {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleCalendarApi::class.java)
    }

    private suspend fun fullSync(
        context: Context,
        api: GoogleCalendarApi,
        dao: LogDao,
        accessToken: String
    ) {
        val allLogs = mutableListOf<LogEntity>()
        var pageToken: String? = null

        do {
            val response = api.listPrimaryEvents(
                authorization = "Bearer $accessToken",
                pageToken = pageToken,
                syncToken = null,
                singleEvents = true,
                orderBy = "startTime",
                showDeleted = true,
                maxResults = 2500
            )

            val logs = response.items
                .filter { it.status != "cancelled" }
                .mapNotNull { it.toLogEntity() }

            allLogs.addAll(logs)
            pageToken = response.nextPageToken
        } while (pageToken != null)

        dao.deleteAllCalendarLogs()

        if (allLogs.isNotEmpty()) {
            dao.insertAll(allLogs)
        }
    }

    private suspend fun incrementalSync(
        context: Context,
        api: GoogleCalendarApi,
        dao: LogDao,
        accessToken: String,
        syncToken: String
    ) {
        try {
            val allLogs = mutableListOf<LogEntity>()
            var pageToken: String? = null
            var nextSyncToken: String? = null

            do {
                val response = api.listPrimaryEvents(
                    authorization = "Bearer $accessToken",
                    pageToken = pageToken,
                    syncToken = syncToken,
                    singleEvents = true,
                    orderBy = null,
                    showDeleted = true,
                    maxResults = 2500
                )

                response.items.forEach { event ->
                    if (event.status != "cancelled") {
                        event.toLogEntity()?.let { allLogs.add(it) }
                    }
                }

                pageToken = response.nextPageToken
                nextSyncToken = response.nextSyncToken
            } while (pageToken != null)

            dao.deleteAllCalendarLogs()

            if (allLogs.isNotEmpty()) {
                dao.insertAll(allLogs)
            }

            if (!nextSyncToken.isNullOrBlank()) {
                TokenManager.saveGoogleCalendarSyncToken(context, nextSyncToken)
            }

        } catch (e: HttpException) {
            if (e.code() == 410) {
                TokenManager.clearGoogleCalendarSyncToken(context)
                dao.deleteAllCalendarLogs()
                fullSync(context, api, dao, accessToken)
            } else {
                throw e
            }
        }
    }

    private fun GoogleCalendarEventDto.toLogEntity(): LogEntity? {
        val startMillis = start?.toEpochMillis() ?: return null
        val endMillis = end?.toEpochMillis()

        val duration = if (endMillis != null) {
            formatCalendarDuration(endMillis - startMillis)
        } else {
            ""
        }

        return LogEntity(
            type = "calendar",
            data = summary ?: "(No title)",
            secondaryData = duration,
            timestamp = startMillis
        )
    }

    private fun GoogleCalendarDateTimeDto.toEpochMillis(): Long? {
        return when {
            dateTime != null -> runCatching {
                OffsetDateTime.parse(dateTime).toInstant().toEpochMilli()
            }.getOrNull()

            date != null -> runCatching {
                LocalDate.parse(date)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()

            else -> null
        }
    }

    private fun formatCalendarDuration(durationMillis: Long): String {
        if (durationMillis <= 0L) return ""

        val totalMinutes = durationMillis / 1000 / 60

        if (totalMinutes == 24L * 60L) {
            return "All day"
        }

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (minutes == 0L) {
            "${hours}h"
        } else {
            "${hours}:${minutes.toString().padStart(2, '0')}h"
        }
    }
}

data class GoogleCalendarEventsResponse(
    @SerializedName("items") val items: List<GoogleCalendarEventDto> = emptyList(),
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("nextSyncToken") val nextSyncToken: String? = null
)

data class GoogleCalendarEventDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("htmlLink") val htmlLink: String? = null,
    @SerializedName("updated") val updated: String? = null,
    @SerializedName("start") val start: GoogleCalendarDateTimeDto? = null,
    @SerializedName("end") val end: GoogleCalendarDateTimeDto? = null
)

data class GoogleCalendarDateTimeDto(
    @SerializedName("dateTime") val dateTime: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("timeZone") val timeZone: String? = null
)