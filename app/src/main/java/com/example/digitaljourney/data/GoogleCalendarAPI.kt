package com.example.digitaljourney.data

import com.example.digitaljourney.data.repositories.GoogleCalendarEventsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface GoogleCalendarApi {

    @GET("calendar/v3/calendars/primary/events")
    suspend fun listPrimaryEvents(
        @Header("Authorization") authorization: String,
        @Query("pageToken") pageToken: String? = null,
        @Query("syncToken") syncToken: String? = null,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String? = "startTime",
        @Query("showDeleted") showDeleted: Boolean = true,
        @Query("maxResults") maxResults: Int = 2500
    ): GoogleCalendarEventsResponse
}