package com.example.digitaljourney.model

import com.example.digitaljourney.data.repositories.PhotosRepositoryImpl
import com.example.digitaljourney.data.repositories.SpotifyRepositoryImpl
import com.example.digitaljourney.data.repositories.LocationRepositoryImpl
import com.example.digitaljourney.data.managers.LogSyncManager
import com.example.digitaljourney.data.repositories.WeatherRepository
import com.example.digitaljourney.data.repositories.CalendarRepository

import android.content.Context
import androidx.room.*
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.Database
import androidx.room.Room
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

// Room Entity
@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val data: String,
    val secondaryData: String,
    val timestamp: Long
)

// DAO
@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT COUNT(*) FROM logs WHERE type = :type AND timestamp = :timestamp")
    suspend fun exists(type: String, timestamp: Long): Int

    @Query("""
    SELECT * FROM logs
    WHERE timestamp >= :start AND timestamp < :end
    ORDER BY timestamp ASC
""")
    fun getLogsForDay(start: Long, end: Long): Flow<List<LogEntity>>

    @Query("""
    SELECT * FROM logs 
    WHERE type = :type 
      AND timestamp >= :startOfDay 
    ORDER BY timestamp DESC 
    LIMIT 1
""")
    suspend fun getLastLogOfTypeToday(type: String, startOfDay: Long): LogEntity?

    @Query("""
    SELECT * FROM logs
    WHERE timestamp >= :start AND timestamp < :end
    ORDER BY timestamp ASC
""")
    fun getLogsForRange(start: Long, end: Long): Flow<List<LogEntity>>

    @Query("""
    SELECT * FROM logs
    WHERE data LIKE '%' || :query || '%'
       OR secondaryData LIKE '%' || :query || '%'
       OR type LIKE '%' || :query || '%'
    ORDER BY timestamp DESC
""")
    fun searchLogs(query: String): List<LogEntity>

    @Query("DELETE FROM logs WHERE type = 'calendar'")
    suspend fun deleteAllCalendarLogs()

    @Query("SELECT * FROM logs WHERE type = 'calendar'")
    suspend fun getAllCalendarLogs(): List<LogEntity>

    @Query("SELECT COUNT(*) FROM logs")
    fun getLogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<LogEntity>)

}


// Database
@Database(entities = [LogEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digitaljourney.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Background Worker
class LogCollectorWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    // gets the most recent logs, meant to run in the background
    override suspend fun doWork(): Result {
        return try {

            val sync = LogSyncManager(
                applicationContext,
                SpotifyRepositoryImpl(),
                PhotosRepositoryImpl(),
                LocationRepositoryImpl(),
                WeatherRepository(),
                CalendarRepository
            )
            sync.syncNow()

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("LogCollectorWorker", "Fetch failed", e)
            Result.retry()
        }
    }
}

