package com.example.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.Task
import com.example.todo.data.TaskRepository
import com.example.todo.data.TodoDatabase
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

    fun addTask(title: String, description: String, dueDate: Long, isRecurring: Boolean = false, recurrencePattern: String? = null) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                dueDate = dueDate,
                isRecurring = isRecurring,
                recurrencePattern = recurrencePattern
            )
            repository.insert(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            if (task.isRecurring) {
                val nextDueDate = calculateNextDueDate(task.dueDate, task.recurrencePattern)
                val updatedTask = task.copy(
                    dueDate = nextDueDate,
                    lastCompletedDate = System.currentTimeMillis()
                )
                repository.update(updatedTask)
            } else {
                repository.update(task.copy(isCompleted = true))
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
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
