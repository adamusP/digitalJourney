package com.example.digitaljourney.model

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.Database
import androidx.room.Room
import com.example.digitaljourney.data.PhotosRepositoryImpl
import com.example.digitaljourney.data.SpotifyRepositoryImpl
import com.example.digitaljourney.data.LocationRepositoryImpl
import com.example.digitaljourney.data.LogSyncManager

import com.example.digitaljourney.data.WeatherRepository

// --- Room Entity ---
@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,       // "spotify", "photo", "location", "mood"
    val data: String,       // JSON payload or simplified string/JSON
    val secondaryData: String,
    val timestamp: Long
)

// --- DAO ---
@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT COUNT(*) FROM logs WHERE type = :type AND timestamp = :timestamp")
    suspend fun exists(type: String, timestamp: Long): Int

    @Query("SELECT * FROM logs WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getLogsForDay(start: Long, end: Long): Flow<List<LogEntity>>

    @Query("""
    SELECT * FROM logs 
    WHERE type = :type 
      AND timestamp >= :startOfDay 
    ORDER BY timestamp DESC 
    LIMIT 1
""")
    suspend fun getLastLogOfTypeToday(type: String, startOfDay: Long): LogEntity?

    @Query("SELECT * FROM logs WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getLogsForRange(start: Long, end: Long): Flow<List<LogEntity>>
}


// --- Database ---
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

// --- Background Worker ---
class LogCollectorWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {

            val sync = LogSyncManager(
                applicationContext,
                SpotifyRepositoryImpl(),
                PhotosRepositoryImpl(),
                LocationRepositoryImpl(),
                WeatherRepository()
            )
            sync.syncNow()

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("LogCollectorWorker", "Location fetch failed", e)
            Result.retry()
        }
    }
}
