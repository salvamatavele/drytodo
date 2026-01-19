package com.example.todo.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todo.MainActivity

class TaskNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskTitle = inputData.getString("TASK_TITLE") ?: "Tarefa pendente"
        val taskId = inputData.getInt("TASK_ID", 0)
        val isPreAlarm = inputData.getBoolean("IS_PRE_ALARM", false)

        val message = if (isPreAlarm) {
            "🚨 Atenção: \"$taskTitle\" em 10 minutos!"
        } else {
            "⏰ HORA DA TAREFA: $taskTitle"
        }

        showNotification(taskTitle, message, taskId)
        return Result.success()
    }

    private fun showNotification(title: String, message: String, id: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_alarm_channel"

        // Create a long repeating vibration pattern (e.g., 1s on, 0.5s off, repeated 20 times ~ 30 seconds)
        val longVibrationPattern = longArrayOf(
            0, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000,
            500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Alarmes de Tarefas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas críticos de tarefas do DRYTODO"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longVibrationPattern
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TASK_ID", id)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            id, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("DRYTODO - ALERTA")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longVibrationPattern)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        val notification = notificationBuilder.build()
        // FLAG_INSISTENT makes the sound and vibration repeat until acknowledged
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        notificationManager.notify(id, notification)
    }
}
