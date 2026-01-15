package com.example.todo.util

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.todo.notification.TaskNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationUtils {
    fun scheduleNotification(context: Context, title: String, time: Long) {
        val workManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putString("TASK_TITLE", title)
            .putInt("TASK_ID", title.hashCode())
            .build()

        val delay = (time - System.currentTimeMillis()).coerceAtLeast(0)
        val notificationWork = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueue(notificationWork)
    }
}
