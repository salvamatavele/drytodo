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
            // Use title hash as unique notification ID for now
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

    fun completeTask(task: Task, context: android.content.Context) {
        viewModelScope.launch {
            if (task.isRecurring) {
                val nextDueDate = calculateNextDueDate(task.dueDate, task.recurrencePattern)
                val updatedTask = task.copy(
                    dueDate = nextDueDate,
                    lastCompletedDate = System.currentTimeMillis()
                )
                repository.update(updatedTask)
                // Reschedule for next occurrence
                NotificationUtils.cancelNotification(context, task.id)
                NotificationUtils.scheduleNotification(context, task.id, task.title, nextDueDate)
            } else {
                repository.update(task.copy(isCompleted = true))
                NotificationUtils.cancelNotification(context, task.id)
            }
        }
    }

    fun deleteTask(task: Task, context: android.content.Context) {
        viewModelScope.launch {
            repository.delete(task)
            NotificationUtils.cancelNotification(context, task.id)
        }
    }

    private fun calculateNextDueDate(currentDueDate: Long, pattern: String?): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDueDate
        when (pattern) {
            "DIÁRIO" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "SEMANAL" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MENSAL" -> calendar.add(Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
