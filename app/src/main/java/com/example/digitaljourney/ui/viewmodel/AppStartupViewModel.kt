package com.example.digitaljourney.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.digitaljourney.data.repositories.CalendarRepository
import com.example.digitaljourney.data.repositories.LocationRepositoryImpl
import com.example.digitaljourney.data.managers.LogSyncManager
import com.example.digitaljourney.data.repositories.PhotosRepositoryImpl
import com.example.digitaljourney.data.repositories.SpotifyRepositoryImpl
import com.example.digitaljourney.data.repositories.WeatherRepository
import com.example.digitaljourney.model.LogCollectorWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppStartupViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    fun initializeApp() {
        scheduleLogCollector()
        runInitialSync()
    }

    private fun scheduleLogCollector() {
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            "log_collector",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<LogCollectorWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    private fun runInitialSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val sync = LogSyncManager(
                appContext,
                SpotifyRepositoryImpl(),
                PhotosRepositoryImpl(),
                LocationRepositoryImpl(),
                WeatherRepository(),
                CalendarRepository
            )
            sync.syncNow()
        }
    }
}