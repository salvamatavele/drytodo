package com.example.todo.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allLogs: Flow<List<TaskLog>> = taskDao.getAllLogs()

    suspend fun insert(task: Task) = taskDao.insertTask(task)
    suspend fun update(task: Task) = taskDao.updateTask(task)
    suspend fun delete(task: Task) = taskDao.deleteTask(task)
    suspend fun getTaskById(id: Int) = taskDao.getTaskById(id)
    
    suspend fun insertLog(log: TaskLog) = taskDao.insertLog(log)
}
