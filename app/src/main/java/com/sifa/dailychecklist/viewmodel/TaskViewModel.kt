package com.sifa.dailychecklist.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sifa.dailychecklist.AppConstants
import com.sifa.dailychecklist.model.Task
import com.sifa.dailychecklist.toJson
import com.sifa.dailychecklist.toTaskList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "TaskViewModel"
    }

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val sharedPreferences: SharedPreferences by lazy {
        getApplication<Application>().getSharedPreferences(
            AppConstants.SHARED_PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val taskList = withContext(Dispatchers.IO) {
                    retrieveTasksFromStorage()
                }
                _tasks.value = taskList
                Log.d(TAG, "Successfully loaded ${taskList.size} tasks")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load tasks", e)
                _error.value = "Failed to load tasks: ${e.message}"
                _tasks.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            try {
                val currentTasks = _tasks.value ?: emptyList()
                val updatedTasks = currentTasks + task

                val success = withContext(Dispatchers.IO) {
                    storeTasksToStorage(updatedTasks)
                }

                if (success) {
                    _tasks.value = updatedTasks
                    _error.value = null
                    Log.d(TAG, "Successfully added task: ${task.name}")
                } else {
                    _error.value = "Failed to save task"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add task", e)
                _error.value = "Failed to add task: ${e.message}"
            }
        }
    }

    fun updateTask(updatedTask: Task) {
        viewModelScope.launch {
            try {
                val currentTasks = _tasks.value ?: emptyList()
                val updatedTasks = currentTasks.map { task ->
                    if (task.id == updatedTask.id) updatedTask else task
                }

                val success = withContext(Dispatchers.IO) {
                    storeTasksToStorage(updatedTasks)
                }

                if (success) {
                    _tasks.value = updatedTasks
                    _error.value = null
                    Log.d(TAG, "Successfully updated task: ${updatedTask.name}")
                } else {
                    _error.value = "Failed to update task"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update task", e)
                _error.value = "Failed to update task: ${e.message}"
            }
        }
    }

    fun toggleTaskCompletion(taskId: Int) {
        viewModelScope.launch {
            try {
                val currentTasks = _tasks.value ?: emptyList()
                val updatedTasks = currentTasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(isCompleted = !task.isCompleted)
                    } else {
                        task
                    }
                }

                val success = withContext(Dispatchers.IO) {
                    storeTasksToStorage(updatedTasks)
                }

                if (success) {
                    _tasks.value = updatedTasks
                    _error.value = null
                    Log.d(TAG, "Successfully toggled completion for task ID: $taskId")
                } else {
                    _error.value = "Failed to update task completion"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle task completion", e)
                _error.value = "Failed to toggle task completion: ${e.message}"
            }
        }
    }

    fun toggleTaskPriority(taskId: Int) {
        viewModelScope.launch {
            try {
                val currentTasks = _tasks.value ?: emptyList()
                val updatedTasks = currentTasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(isPriority = !task.isPriority)
                    } else {
                        task
                    }
                }

                val success = withContext(Dispatchers.IO) {
                    storeTasksToStorage(updatedTasks)
                }

                if (success) {
                    _tasks.value = updatedTasks
                    _error.value = null
                    Log.d(TAG, "Successfully toggled priority for task ID: $taskId")
                } else {
                    _error.value = "Failed to update task priority"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle task priority", e)
                _error.value = "Failed to toggle task priority: ${e.message}"
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                val currentTasks = _tasks.value ?: emptyList()
                val updatedTasks = currentTasks.filter { it.id != taskId }

                val success = withContext(Dispatchers.IO) {
                    storeTasksToStorage(updatedTasks)
                }

                if (success) {
                    _tasks.value = updatedTasks
                    _error.value = null
                    Log.d(TAG, "Successfully deleted task ID: $taskId")
                } else {
                    _error.value = "Failed to delete task"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete task", e)
                _error.value = "Failed to delete task: ${e.message}"
            }
        }
    }

    // Bulk operations
    fun updateAllTasks(tasks: List<Task>) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    storeTasksToStorage(tasks)
                }

                if (success) {
                    _tasks.value = tasks
                    _error.value = null
                    Log.d(TAG, "Successfully updated all tasks (${tasks.size} tasks)")
                } else {
                    _error.value = "Failed to update tasks"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update all tasks", e)
                _error.value = "Failed to update tasks: ${e.message}"
            }
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    storeTasksToStorage(emptyList())
                }

                if (success) {
                    _tasks.value = emptyList()
                    _error.value = null
                    Log.d(TAG, "Successfully cleared all tasks")
                } else {
                    _error.value = "Failed to clear tasks"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all tasks", e)
                _error.value = "Failed to clear tasks: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun refreshTasks() {
        loadTasks()
    }

    // Helper functions for data persistence
    private suspend fun storeTasksToStorage(tasks: List<Task>): Boolean {
        return try {
            val jsonString = tasks.toJson()
            if (jsonString != null) {
                val editor = sharedPreferences.edit()
                editor.putString(AppConstants.TASK_LIST_KEY, jsonString)
                val success = editor.commit() // Use commit() for synchronous operation
                Log.d(TAG, "Stored ${tasks.size} tasks to storage: $success")
                success
            } else {
                Log.e(TAG, "Failed to serialize tasks to JSON")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing tasks to storage", e)
            false
        }
    }

    private suspend fun retrieveTasksFromStorage(): List<Task> {
        return try {
            val jsonString = sharedPreferences.getString(AppConstants.TASK_LIST_KEY, null)
            val tasks = jsonString?.toTaskList() ?: emptyList()
            Log.d(TAG, "Retrieved ${tasks.size} tasks from storage")
            tasks
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving tasks from storage", e)
            emptyList()
        }
    }

    // Utility functions for UI
    fun getCompletedTasksCount(): Int = _tasks.value?.count { it.isCompleted } ?: 0
    fun getTotalTasksCount(): Int = _tasks.value?.size ?: 0
    fun getPriorityTasksCount(): Int = _tasks.value?.count { it.isPriority } ?: 0
    fun getTaskById(id: Int): Task? = _tasks.value?.find { it.id == id }
}