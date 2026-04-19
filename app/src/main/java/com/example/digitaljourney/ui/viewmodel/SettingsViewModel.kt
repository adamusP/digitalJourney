package com.example.digitaljourney.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaljourney.data.managers.TokenManager
import com.example.digitaljourney.data.repositories.LogRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val logRepository = LogRepository(application)

    private val _totalLogs = mutableStateOf(0)
    val totalLogs: State<Int> = _totalLogs

    fun loadTotalLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _totalLogs.value = logRepository.getAllLogsCount()
        }
    }

    fun saveChessUsername(username: String) {
        val trimmed = username.trim()
        if (trimmed.isNotEmpty()) {
            TokenManager.saveChessUsername(appContext, trimmed)
        }
    }

    fun saveLetrUsername(username: String) {
        val trimmed = username.trim()
        if (trimmed.isNotEmpty()) {
            TokenManager.saveLetrUsername(appContext, trimmed)
        }
    }

    fun getChessUsername(): String {
        return TokenManager.getChessUsername(appContext) ?: ""
    }

    fun getLetrUsername() : String {
        return TokenManager.getLetrUsername(appContext) ?: ""
    }


}