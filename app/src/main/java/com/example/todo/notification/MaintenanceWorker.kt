package com.example.todo.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todo.data.TodoDatabase
import com.example.todo.data.Task
import com.example.todo.data.TaskLog
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
        
        // 8 hours margin as per previous requirement or immediate?
        // User says "caso nao tenha sido concluida registe a nao execucao tambem e passe a para o promo dia"
        val overdue = tasks.filter { it.dueDate < now && !it.isCompleted }
        
        overdue.forEach { task ->
            // 1. Log the performance (or lack of it)
            val log = TaskLog(
                taskId = task.id,
                taskTitle = task.title,
                date = task.dueDate,
                completionPercentage = task.completionPercentage,
                wasCompleted = false
            )
            taskDao.insertLog(log)

            // 2. Calculate next date
            val nextDueDate = if (task.isRecurring) {
                calculateNextRecurrenceDate(task)
            } else {
                calculateNextDay(task.dueDate)
            }
            
            // 3. Update task
            val updatedTask = task.copy(dueDate = nextDueDate)
            taskDao.updateTask(updatedTask)
            
            // 4. Reschedule notifications
            NotificationUtils.cancelNotification(applicationContext, task.id)
            NotificationUtils.scheduleNotification(applicationContext, task.id, task.title, nextDueDate)
        }
        
        return Result.success()
    }

    private fun calculateNextRecurrenceDate(task: Task): Long {
        val calendar = Calendar.getInstance()
        val originalCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
        
        calendar.set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, originalCal.get(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        when (task.recurrencePattern) {
            "SEMANAL" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MENSAL" -> calendar.add(Calendar.MONTH, 1)
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1) // Default is next day for non-recurring or DAILY
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

    private fun calculateNextDay(currentDate: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        
        // Ensure it's in the future
        while (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
