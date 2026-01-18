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
        
        // Use dueDate to determine if a task is overdue
        val overdue = tasks.filter { it.dueDate < now && !it.isCompleted }
        
        overdue.forEach { task ->
            val nextDueDate = calculateNextAutoDueDate(task)
            val updatedTask = task.copy(dueDate = nextDueDate)
            taskDao.updateTask(updatedTask)
            
            NotificationUtils.cancelNotification(applicationContext, task.id)
            NotificationUtils.scheduleNotification(applicationContext, task.id, task.title, nextDueDate)
        }
        
        return Result.success()
    }

    private fun calculateNextAutoDueDate(task: Task): Long {
        val calendar = Calendar.getInstance()
        val originalCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
        
        calendar.set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, originalCal.get(Calendar.MINUTE))
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
