package com.example.digitaljourney.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.digitaljourney.data.repositories.CallRepository
import com.example.digitaljourney.data.repositories.CallRepositoryImpl
import com.example.digitaljourney.data.repositories.LogRepository
import com.example.digitaljourney.data.repositories.PhotosRepository
import com.example.digitaljourney.data.repositories.PhotosRepositoryImpl
import com.example.digitaljourney.model.LogEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MonthViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val logRepository = LogRepository(application)
    private val photosRepo: PhotosRepository = PhotosRepositoryImpl()
    private val callRepo: CallRepository = CallRepositoryImpl()

    private val _selectedMonth = mutableStateOf(LocalDate.now().withDayOfMonth(1))
    val selectedMonth: State<LocalDate> = _selectedMonth

    private val _selectedFilter = mutableStateOf("Off")
    val selectedFilter: State<String> = _selectedFilter

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun getLogsForMonth(month: LocalDate): Flow<List<LogEntity>> {
        val start = month.withDayOfMonth(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val end = month.plusMonths(1).withDayOfMonth(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        return logRepository.getLogsForRange(start, end)
    }

    fun getCountForDay(
        dayDate: LocalDate,
        logsByDay: Map<LocalDate, List<LogEntity>>
    ): Int {
        val entries = logsByDay[dayDate].orEmpty()

        return when (_selectedFilter.value) {
            "Off" -> 0
            "All" -> {
                entries.size +
                        photosRepo.fetchPhotosForDate(appContext, dayDate).size +
                        photosRepo.fetchVideosForDate(appContext, dayDate).size +
                        callRepo.fetchCallsForDate(appContext, dayDate).size
            }
            "photo" -> photosRepo.fetchPhotosForDate(appContext, dayDate).size
            "video" -> photosRepo.fetchVideosForDate(appContext, dayDate).size
            "call" -> callRepo.fetchCallsForDate(appContext, dayDate).size
            else -> entries.count { it.type == _selectedFilter.value }
        }
    }

    fun buildLogsByDay(logs: List<LogEntity>): Map<LocalDate, List<LogEntity>> {
        return logs.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(ZoneOffset.UTC).toLocalDate()
        }
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun setMonth(newMonth: LocalDate) {
        _selectedMonth.value = newMonth.withDayOfMonth(1)
    }
}