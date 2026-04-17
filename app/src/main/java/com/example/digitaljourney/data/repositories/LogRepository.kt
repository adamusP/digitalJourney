package com.example.digitaljourney.data.repositories

import android.content.Context
import com.example.digitaljourney.model.AppDatabase
import com.example.digitaljourney.model.LogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LogRepository(private val context: Context) {

    private val logDao = AppDatabase.getInstance(context).logDao()

    fun getAllLogsCount(): Int {
        return logDao.getLogCount()
    }

    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<LogEntity>> {
        return logDao.getLogsForDay(startOfDay, endOfDay)
    }

    fun getLogsForRange(start: Long, end: Long): Flow<List<LogEntity>> {
        return logDao.getLogsForRange(start, end)
    }

    suspend fun searchLogs(query: String): List<LogEntity> {
        return withContext(Dispatchers.IO) {
            logDao.searchLogs(query)
        }
    }

    suspend fun insertMood(emoji: String, description: String) {
        withContext(Dispatchers.IO) {
            logDao.insert(
                LogEntity(
                    type = "mood",
                    data = emoji,
                    secondaryData = description,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun insertText(text: String) {
        withContext(Dispatchers.IO) {
            logDao.insert(
                LogEntity(
                    type = "text",
                    data = text,
                    secondaryData = "",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}