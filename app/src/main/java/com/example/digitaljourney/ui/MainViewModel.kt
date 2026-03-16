package com.example.digitaljourney.ui

import com.example.digitaljourney.data.*
import com.example.digitaljourney.model.*
import com.example.digitaljourney.data.CallRepository

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.Instant

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val photosRepo: PhotosRepository = PhotosRepositoryImpl()
    val photosForDay = mutableStateOf<List<LogEntry.PhotoLog>>(emptyList())
    val videosForDay = mutableStateOf<List<LogEntry.VideoLog>>(emptyList())

    private val callRepo: CallRepository = CallRepositoryImpl()
    val callLogsForDay = mutableStateOf<List<LogEntry.CallLog>>(emptyList())

    private val _selectedMonth = mutableStateOf(LocalDate.now().withDayOfMonth(1))
    val selectedMonth: State<LocalDate> = _selectedMonth

    private val _searchResults = mutableStateOf<List<LogEntity>>(emptyList())
    val searchResults: State<List<LogEntity>> = _searchResults

    // database with application context
    private val db = androidx.room.Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "digitaljourney.db"
    ).build()

    val logs: Flow<List<LogEntity>> = db.logDao().getAllLogs()

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    // Holds the logs for the currently selected day
    private val _logsForDay = kotlinx.coroutines.flow.MutableStateFlow<List<LogEntity>>(emptyList())
    val logsForDay: kotlinx.coroutines.flow.StateFlow<List<LogEntity>> = _logsForDay

    private val _highlightedLogTimestamp = mutableStateOf<Long?>(null)
    val highlightedLogTimestamp: State<Long?> = _highlightedLogTimestamp

    private val logDao = AppDatabase.getInstance(application).logDao()

    val selectedFilter = mutableStateOf("Off")


    init {
        loadLogsForDate(_selectedDate.value) // load today initially
    }

    fun loadPhotosForDate(context: Context, date: LocalDate) {
        photosForDay.value = photosRepo.fetchPhotosForDate(context, date)
    }

    fun loadVideosForDate(context: Context, date: LocalDate) {
        videosForDay.value = photosRepo.fetchVideosForDate(context, date)
    }

    fun loadCallLogsForDate(context: Context, date: LocalDate) {
        callLogsForDay.value = callRepo.fetchCallsForDate(context, date)
    }

    fun loadLogsForDate(date: LocalDate) {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            db.logDao().getLogsForDay(startOfDay, endOfDay).collect { logs ->
                _logsForDay.value = logs
            }
        }
    }

    fun search(context: Context, query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbResults = db.logDao().searchLogs(query)

            val callResults = callRepo.searchCalls(context, query).map { call ->
                LogEntity(
                    timestamp = call.time,
                    type = "call",
                    data = "${call.number} ${call.callType} ${call.duration}",
                    secondaryData = ""
                )
            }

            val merged = (dbResults + callResults)
                .sortedByDescending { it.timestamp }

            _searchResults.value = merged
        }
    }

    fun setDate(newDate: LocalDate) {
        _selectedDate.value = newDate
        loadLogsForDate(newDate)
    }

    fun previousDay() {
        setDate(_selectedDate.value.minusDays(1))
    }

    fun nextDay() {
        setDate(_selectedDate.value.plusDays(1))
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun goToLogDay(timestamp: Long) {
        _highlightedLogTimestamp.value = timestamp

        val date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        setDate(date)
    }

    fun clearHighlightedLog() {
        _highlightedLogTimestamp.value = null
    }

    // Observe logs for the entire month
    fun getLogsForMonth(month: LocalDate): Flow<List<LogEntity>> {
        val start = month.withDayOfMonth(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val end = month.plusMonths(1).withDayOfMonth(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        return logDao.getLogsForRange(start, end)
    }

}


