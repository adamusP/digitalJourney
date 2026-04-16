package com.example.digitaljourney.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaljourney.data.repositories.LogRepository
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val logRepository = LogRepository(application)

    fun logMood(emoji: String, description: String) {
        viewModelScope.launch {
            logRepository.insertMood(emoji, description)
        }
    }

    fun logText(text: String, onDone: () -> Unit = {}) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            logRepository.insertText(trimmed)
            onDone()
        }
    }
}