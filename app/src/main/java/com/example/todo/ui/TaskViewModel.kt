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
                val overdue = tasks.filter { it.endDate < now && !it.isCompleted }
                if (overdue.isNotEmpty()) {
                    overdue.forEach { task ->
                        val duration = task.endDate - task.startDate
                        val newStartDate = calculateNextAutoStartDate(task)
                        val newEndDate = newStartDate + duration
                        
                        val updatedTask = task.copy(startDate = newStartDate, endDate = newEndDate)
                        repository.update(updatedTask)
                        
                        NotificationUtils.cancelNotification(context, task.id)
                        NotificationUtils.scheduleNotification(context, task.id, task.title, newStartDate)
                    }
                }
            }
        }
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

    fun addTaskWithNotification(
        context: android.content.Context,
        title: String,
        description: String,
        startDate: Long,
        endDate: Long,
        isRecurring: Boolean = false,
        recurrencePattern: String? = null,
        priority: String = "NORMAL",
        category: String = "Pessoal"
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                startDate = startDate,
                endDate = endDate,
                isRecurring = isRecurring,
                recurrencePattern = recurrencePattern,
                priority = priority,
                category = category
            )
            repository.insert(task)
            NotificationUtils.scheduleNotification(context, title.hashCode(), title, startDate)
        }
    }

    fun updateTask(task: Task, context: android.content.Context) {
        viewModelScope.launch {
            repository.update(task)
            NotificationUtils.cancelNotification(context, task.id)
            if (!task.isCompleted) {
                NotificationUtils.scheduleNotification(context, task.id, task.title, task.startDate)
            }
        }
    }

    fun toggleTaskCompletion(task: Task, context: android.content.Context) {
        viewModelScope.launch {
            if (!task.isCompleted) {
                if (task.isRecurring) {
                    val duration = task.endDate - task.startDate
                    val nextStart = calculateNextPatternDate(task.startDate, task.recurrencePattern)
                    val updatedTask = task.copy(
                        startDate = nextStart,
                        endDate = nextStart + duration,
                        lastCompletedDate = System.currentTimeMillis()
                    )
                    repository.update(updatedTask)
                    NotificationUtils.cancelNotification(context, task.id)
                    NotificationUtils.scheduleNotification(context, task.id, task.title, nextStart)
                } else {
                    repository.update(task.copy(isCompleted = true))
                    NotificationUtils.cancelNotification(context, task.id)
                }
            } else {
                repository.update(task.copy(isCompleted = false))
                NotificationUtils.scheduleNotification(context, task.id, task.title, task.startDate)
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
            val tasksToReschedule = allTasks.value.filter { it.endDate < now && !it.isCompleted }
            tasksToReschedule.forEach { task ->
                val duration = task.endDate - task.startDate
                val newStartDate = calculateNextAutoStartDate(task)
                val newEndDate = newStartDate + duration
                
                val updatedTask = task.copy(startDate = newStartDate, endDate = newEndDate)
                repository.update(updatedTask)
                NotificationUtils.cancelNotification(context, task.id)
                NotificationUtils.scheduleNotification(context, task.id, task.title, newStartDate)
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
