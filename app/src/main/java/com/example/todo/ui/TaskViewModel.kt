package com.example.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.Task
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

    init {
        val taskDao = TodoDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        allTasks = repository.allTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        autoRescheduleOverdue(application)
    }

    private fun autoRescheduleOverdue(context: android.content.Context) {
        viewModelScope.launch {
            allTasks.collect { tasks ->
                val now = System.currentTimeMillis()
                val eightHoursInMs = 8 * 60 * 60 * 1000L
                
                // Intelligent Auto-Reschedule:
                // Only move recurring tasks if they are overdue by more than 8 hours
                val overdueRecurring = tasks.filter { 
                    it.isRecurring && !it.isCompleted && (now > it.dueDate + eightHoursInMs) 
                }
                
                if (overdueRecurring.isNotEmpty()) {
                    overdueRecurring.forEach { task ->
                        val nextDueDate = calculateNextAutoDueDate(task)
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
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1) // Default or DIÁRIO
        }
        
        // If the calculated next date is still in the past, keep jumping forward
        while (calendar.timeInMillis < System.currentTimeMillis()) {
            when (task.recurrencePattern) {
                "SEMANAL" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "MENSAL" -> calendar.add(Calendar.MONTH, 1)
                else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
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
                category = category
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

    fun toggleTaskCompletion(task: Task, context: android.content.Context) {
        viewModelScope.launch {
            if (!task.isCompleted) {
                if (task.isRecurring) {
                    val nextDueDate = calculateNextPatternDate(task.dueDate, task.recurrencePattern)
                    val updatedTask = task.copy(
                        dueDate = nextDueDate,
                        lastCompletedDate = System.currentTimeMillis()
                    )
                    repository.update(updatedTask)
                    NotificationUtils.cancelNotification(context, task.id)
                    NotificationUtils.scheduleNotification(context, task.id, task.title, nextDueDate)
                } else {
                    repository.update(task.copy(isCompleted = true))
                    NotificationUtils.cancelNotification(context, task.id)
                }
            } else {
                repository.update(task.copy(isCompleted = false))
                NotificationUtils.scheduleNotification(context, task.id, task.title, task.dueDate)
            }
        }
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
            val tasksToReschedule = allTasks.value.filter { it.dueDate < now && !it.isCompleted }
            tasksToReschedule.forEach { task ->
                val nextDueDate = calculateNextAutoDueDate(task)
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
