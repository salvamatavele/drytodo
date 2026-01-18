package com.example.todo.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todo.data.TodoDatabase
import com.example.todo.data.Task
import com.example.todo.util.NotificationUtils
import java.util.Calendar

class MaintenanceWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = TodoDatabase.getDatabase(applicationContext)
        val taskDao = database.taskDao()
        
        val tasks = taskDao.getAllTasksList()
        val now = System.currentTimeMillis()
        
        // Use endDate to determine if a task is overdue
        val overdue = tasks.filter { it.endDate < now && !it.isCompleted }
        
        overdue.forEach { task ->
            val duration = task.endDate - task.startDate
            val nextStartDate = calculateNextAutoStartDate(task)
            val nextEndDate = nextStartDate + duration
            
            val updatedTask = task.copy(startDate = nextStartDate, endDate = nextEndDate)
            taskDao.updateTask(updatedTask)
            
            NotificationUtils.cancelNotification(applicationContext, task.id)
            NotificationUtils.scheduleNotification(applicationContext, task.id, task.title, nextStartDate)
        }
        
        return Result.success()
    }

    private fun calculateNextAutoStartDate(task: Task): Long {
        val calendar = Calendar.getInstance()
        val originalStart = Calendar.getInstance().apply { timeInMillis = task.startDate }
        
        calendar.set(Calendar.HOUR_OF_DAY, originalStart.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, originalStart.get(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, 0)
        
        when (task.recurrencePattern) {
            "SEMANAL" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MENSAL" -> calendar.add(Calendar.MONTH, 1)
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        while (calendar.timeInMillis < System.currentTimeMillis()) {
            when (task.recurrencePattern) {
                "SEMANAL" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "MENSAL" -> calendar.add(Calendar.MONTH, 1)
                else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        return calendar.timeInMillis
    }
}
