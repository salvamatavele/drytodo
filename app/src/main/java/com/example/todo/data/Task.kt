package com.example.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val dueDate: Long,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null, // e.g., "DAILY", "WEEKLY", "MENSAL"
    val lastCompletedDate: Long? = null,
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH, URGENT
    val category: String = "Pessoal",
    val completionPercentage: Int = 0
)

@Entity(tableName = "task_logs")
data class TaskLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Int = 0,
    val taskId: Int,
    val taskTitle: String,
    val date: Long,
    val completionPercentage: Int,
    val wasCompleted: Boolean
)
