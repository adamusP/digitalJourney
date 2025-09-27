package com.example.digitaljourney.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.digitaljourney.data.*
import com.example.digitaljourney.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val locationRepo: LocationRepository = LocationRepositoryImpl()
    val lastLocation = mutableStateOf<LogEntry.LocationLog?>(null)

    private val photosRepo: PhotosRepository = PhotosRepositoryImpl()
    val todayPhotos = mutableStateOf<List<LogEntry.PhotoLog>>(emptyList())
    val photosForDay = mutableStateOf<List<LogEntry.PhotoLog>>(emptyList())

    private val spotifyRepo: SpotifyRepository = SpotifyRepositoryImpl()
    val lastPlayedSong = mutableStateOf<String?>(null)

    private val _selectedMonth = mutableStateOf(LocalDate.now().withDayOfMonth(1))
    val selectedMonth: State<LocalDate> = _selectedMonth

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

    init {
        loadLogsForDate(_selectedDate.value) // load today initially
    }

    fun loadLocation(activity: Activity) {
        locationRepo.fetchLastKnownLocation(activity) { log ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                lastLocation.value = log
            }
        }
    }

    fun loadTodayPhotos(context: Context) {
        todayPhotos.value = photosRepo.fetchTodayPhotos(context)
    }

    fun loadPhotosForDate(context: Context, date: LocalDate) {
        photosForDay.value = photosRepo.fetchPhotosForDate(context, date)
    }

    fun fetchLastPlayedSongsToday(token: String) {
        spotifyRepo.fetchTodayLogs(token) { logs ->
            val result = if (logs.isNotEmpty()) {
                "Played today:\n" + logs.joinToString("\n") { "${it.song} – ${it.artist}" }
            } else {
                "No songs played today."
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                lastPlayedSong.value = result
            }
        }
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

}
