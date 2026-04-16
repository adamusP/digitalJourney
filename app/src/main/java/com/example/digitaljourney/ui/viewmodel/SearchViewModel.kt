package com.example.digitaljourney.ui.viewmodel

import com.example.digitaljourney.model.*
import com.example.digitaljourney.data.repositories.CallRepository

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.example.digitaljourney.data.repositories.CallRepositoryImpl
import com.example.digitaljourney.data.repositories.LogRepository

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val logRepository = LogRepository(application)
    private val callRepo: CallRepository = CallRepositoryImpl()

    private val _searchResults = mutableStateOf<List<LogEntity>>(emptyList())
    val searchResults: State<List<LogEntity>> = _searchResults

    fun search(context: Context, query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbResults = logRepository.searchLogs(query)

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

}


