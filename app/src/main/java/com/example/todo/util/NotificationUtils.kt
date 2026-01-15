package com.example.todo.util

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.todo.notification.TaskNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationUtils {
    fun scheduleNotification(context: Context, id: Int, title: String, time: Long) {
        val workManager = WorkManager.getInstance(context)
        
        // 1. Notification for "ON TIME"
        val onTimeData = Data.Builder()
            .putString("TASK_TITLE", title)
            .putInt("TASK_ID", id)
            .putBoolean("IS_PRE_ALARM", false)
            .build()

        val onTimeDelay = (time - System.currentTimeMillis()).coerceAtLeast(0)
        val onTimeWork = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(onTimeDelay, TimeUnit.MILLISECONDS)
            .setInputData(onTimeData)
            .addTag("task_$id")
            .build()

        // 2. Notification for "10 MIN BEFORE"
        val preAlarmTime = time - (10 * 60 * 1000) // 10 minutes in ms
        val preAlarmDelay = (preAlarmTime - System.currentTimeMillis()).coerceAtLeast(0)
        
        if (preAlarmDelay > 0) {
            val preAlarmData = Data.Builder()
                .putString("TASK_TITLE", title)
                .putInt("TASK_ID", id + 1000000) // Unique ID for pre-alarm
                .putBoolean("IS_PRE_ALARM", true)
                .build()

            val preAlarmWork = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
                .setInitialDelay(preAlarmDelay, TimeUnit.MILLISECONDS)
                .setInputData(preAlarmData)
                .addTag("task_pre_$id")
                .build()
            
            workManager.enqueue(preAlarmWork)
        }

        workManager.enqueue(onTimeWork)
    }

    fun cancelNotification(context: Context, id: Int) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("task_$id")
        workManager.cancelAllWorkByTag("task_pre_$id")
    }
}
