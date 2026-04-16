package com.example.digitaljourney.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaljourney.data.repositories.CallRepository
import com.example.digitaljourney.data.repositories.CallRepositoryImpl
import com.example.digitaljourney.data.repositories.LogRepository
import com.example.digitaljourney.data.repositories.PhotosRepository
import com.example.digitaljourney.data.repositories.PhotosRepositoryImpl
import com.example.digitaljourney.data.repositories.LocationRepositoryImpl
import com.example.digitaljourney.data.repositories.CalendarRepository
import com.example.digitaljourney.data.managers.LogSyncManager
import com.example.digitaljourney.model.LogEntity
import com.example.digitaljourney.model.LogEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

import com.example.digitaljourney.data.repositories.SpotifyRepositoryImpl
import com.example.digitaljourney.data.repositories.WeatherRepository
import kotlinx.coroutines.Dispatchers



class DayViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val syncManager = LogSyncManager(
        appContext,
        SpotifyRepositoryImpl(),
        PhotosRepositoryImpl(),
        LocationRepositoryImpl(),
        WeatherRepository(),
        CalendarRepository
    )

    private var logsForDayJob: Job? = null

    private val logRepository = LogRepository(application)
    private val photosRepo: PhotosRepository = PhotosRepositoryImpl()
    private val callRepo: CallRepository = CallRepositoryImpl()



    private val _logsForDay = MutableStateFlow<List<LogEntity>>(emptyList())
    val logsForDay: StateFlow<List<LogEntity>> = _logsForDay

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    private val _highlightedLogTimestamp = mutableStateOf<Long?>(null)
    val highlightedLogTimestamp: State<Long?> = _highlightedLogTimestamp

    private val _photosForDay = mutableStateOf<List<LogEntry.PhotoLog>>(emptyList())
    val photosForDay: State<List<LogEntry.PhotoLog>> = _photosForDay

    private val _videosForDay = mutableStateOf<List<LogEntry.VideoLog>>(emptyList())
    val videosForDay: State<List<LogEntry.VideoLog>> = _videosForDay

    private val _callLogsForDay = mutableStateOf<List<LogEntry.CallLog>>(emptyList())
    val callLogsForDay: State<List<LogEntry.CallLog>> = _callLogsForDay

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    init {
        loadLogsForDate(_selectedDate.value)
    }

    fun setDate(newDate: LocalDate) {
        _selectedDate.value = newDate
        loadLogsForDate(newDate)
    }

    fun loadLogsForDate(date: LocalDate) {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        logsForDayJob?.cancel()
        logsForDayJob = viewModelScope.launch {
            logRepository.getLogsForDay(startOfDay, endOfDay).collect { logs ->
                _logsForDay.value = logs
            }
        }
    }

    fun previousDay() {
        setDate(_selectedDate.value.minusDays(1))
    }

    fun nextDay() {
        setDate(_selectedDate.value.plusDays(1))
    }

    fun loadPhotosForDate(context: Context, date: LocalDate) {
        _photosForDay.value = photosRepo.fetchPhotosForDate(context, date)
    }

    fun loadVideosForDate(context: Context, date: LocalDate) {
        _videosForDay.value = photosRepo.fetchVideosForDate(context, date)
    }

    fun loadCallLogsForDate(context: Context, date: LocalDate) {
        _callLogsForDay.value = callRepo.fetchCallsForDate(context, date)
    }

    fun loadExtraLogsForSelectedDate(context: Context) {
        val date = _selectedDate.value
        loadPhotosForDate(context, date)
        loadVideosForDate(context, date)
        loadCallLogsForDate(context, date)
    }

    fun refreshSelectedDay(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                syncManager.syncNow()
                loadExtraLogsForSelectedDate(context)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun highlightLog(timestamp: Long) {
        _highlightedLogTimestamp.value = timestamp
    }

    fun clearHighlightedLog() {
        _highlightedLogTimestamp.value = null
    }
}