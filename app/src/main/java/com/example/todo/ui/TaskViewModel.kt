package com.example.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.Task
import com.example.todo.data.TaskLog
import com.example.todo.data.TaskRepository
import com.example.todo.data.TodoDatabase
import com.example.todo.util.NotificationUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    val allTasks: StateFlow<List<Task>>
    val allLogs: StateFlow<List<TaskLog>>

    init {
        val taskDao = TodoDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        allTasks = repository.allTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allLogs = repository.allLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        autoRescheduleOverdue(application)
    }

    private fun autoRescheduleOverdue(context: android.content.Context) {
        viewModelScope.launch {
            allTasks.collect { tasks ->
                val now = System.currentTimeMillis()
                val eightHoursInMs = 8 * 60 * 60 * 1000L
                
                // Only reschedule if at least 8 hours have passed since the due date
                val overdue = tasks.filter { it.dueDate + eightHoursInMs < now && !it.isCompleted }
                
                if (overdue.isNotEmpty()) {
                    overdue.forEach { task ->
                        // Log the non-completion in history
                        repository.insertLog(TaskLog(
                            taskId = task.id,
                            taskTitle = task.title,
                            date = task.dueDate,
                            completionPercentage = task.completionPercentage,
                            wasCompleted = false
                        ))

                        val nextDueDate = if (task.isRecurring) {
                            calculateNextAutoDueDate(task)
                        } else {
                            calculateNextDay(task.dueDate)
                        }
                        
                        val updatedTask = task.copy(dueDate = nextDueDate)
                        repository.update(updatedTask)
                        
                        NotificationUtils.cancelNotification(context, task.id)
                        NotificationUtils.scheduleNotification(context, task.id, task.title, nextDueDate)
                    }
                }
            }
        }
    }

    private fun calculateNextAutoDueDate(task: Task): Long {
        val calendar = Calendar.getInstance()
        val originalCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
        
        calendar.set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, originalCal.get(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
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

    private fun calculateNextDay(currentDate: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        while (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun addTaskWithNotification(
        context: android.content.Context,
        title: String,
        description: String,
        dueDate: Long,
        isRecurring: Boolean = false,
        recurrencePattern: String? = null,
        priority: String = "NORMAL",
        category: String = "Pessoal"
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                dueDate = dueDate,
                isRecurring = isRecurring,
                recurrencePattern = recurrencePattern,
                priority = priority,
                category = category,
                completionPercentage = 0
            )
            repository.insert(task)
            NotificationUtils.scheduleNotification(context, title.hashCode(), title, dueDate)
        }
    }

    fun updateTask(task: Task, context: android.content.Context) {
        viewModelScope.launch {
            repository.update(task)
            NotificationUtils.cancelNotification(context, task.id)
            if (!task.isCompleted) {
                NotificationUtils.scheduleNotification(context, task.id, task.title, task.dueDate)
            }
        }
    }

    fun updateTaskPercentage(task: Task, percentage: Int, context: android.content.Context) {
        viewModelScope.launch {
            val isNewlyCompleted = percentage == 100
            
            if (isNewlyCompleted) {
                // Log completion
                repository.insertLog(TaskLog(
                    taskId = task.id,
                    taskTitle = task.title,
                    date = System.currentTimeMillis(),
                    completionPercentage = 100,
                    wasCompleted = true
                ))

                if (task.isRecurring) {
                    val nextDueDate = calculateNextPatternDate(task.dueDate, task.recurrencePattern)
                    val updatedTask = task.copy(
                        dueDate = nextDueDate,
                        lastCompletedDate = System.currentTimeMillis(),
                        isCompleted = false,
                        completionPercentage = 0 // Reset for next instance
                    )
                    repository.update(updatedTask)
                    NotificationUtils.cancelNotification(context, task.id)
                    NotificationUtils.scheduleNotification(context, task.id, task.title, nextDueDate)
                } else {
                    repository.update(task.copy(isCompleted = true, completionPercentage = 100))
                    NotificationUtils.cancelNotification(context, task.id)
                }
            } else {
                repository.update(task.copy(isCompleted = false, completionPercentage = percentage))
                // Reschedule notification if it was completed and now is not
                if (task.isCompleted) {
                    NotificationUtils.scheduleNotification(context, task.id, task.title, task.dueDate)
                }
            }
        }
    }

    fun toggleTaskCompletion(task: Task, context: android.content.Context) {
        val newPercentage = if (task.isCompleted) 0 else 100
        updateTaskPercentage(task, newPercentage, context)
    }

    fun deleteTask(task: Task, context: android.content.Context) {
        viewModelScope.launch {
            repository.delete(task)
            NotificationUtils.cancelNotification(context, task.id)
        }
    }

    fun rescheduleOverdueTasks(context: android.content.Context) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val eightHoursInMs = 8 * 60 * 60 * 1000L
            
            // Manual trigger still respects the 8-hour rule or forces it? 
            // Let's make manual trigger force it but keep the 8-hour rule for auto.
            // Actually, usually manual means "do it now".
            val tasksToReschedule = allTasks.value.filter { it.dueDate < now && !it.isCompleted }
            tasksToReschedule.forEach { task ->
                repository.insertLog(TaskLog(
                    taskId = task.id,
                    taskTitle = task.title,
                    date = task.dueDate,
                    completionPercentage = task.completionPercentage,
                    wasCompleted = false
                ))

                val nextDueDate = if (task.isRecurring) {
                    calculateNextAutoDueDate(task)
                } else {
                    calculateNextDay(task.dueDate)
                }

                val updatedTask = task.copy(dueDate = nextDueDate)
                repository.update(updatedTask)
                NotificationUtils.cancelNotification(context, task.id)
                NotificationUtils.scheduleNotification(context, task.id, task.title, nextDueDate)
            }
        }
    }

    private fun calculateNextPatternDate(current: Long, pattern: String?): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = current }
        when (pattern) {
            "DIÁRIO" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "SEMANAL" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MENSAL" -> calendar.add(Calendar.MONTH, 1)
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
