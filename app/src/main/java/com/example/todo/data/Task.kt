package com.example.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val startDate: Long,
    val endDate: Long,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null, // e.g., "DAILY", "WEEKLY", "MENSAL"
    val lastCompletedDate: Long? = null,
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH, URGENT
    val category: String = "Pessoal" // Pessoal, Trabalho, Saúde, etc.
)
