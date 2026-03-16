package com.example.digitaljourney.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// schedules all notifications
object NotificationScheduler {
    private const val WORK_NAME = "daily_log_reminder"

    fun scheduleDailyReminder(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            12, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}