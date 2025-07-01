package com.sifa.dailychecklist

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sifa.dailychecklist.model.Task
import com.sifa.dailychecklist.ui.MainScreen
import com.sifa.dailychecklist.ui.theme.DailyChecklistTheme
import com.sifa.dailychecklist.viewmodel.TaskViewModel
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit



object AppConstants {
    const val SHARED_PREFS_NAME = "MyPreferences"
    const val TASK_LIST_KEY = "myTaskList"
    const val DAILY_RESET_WORK_NAME = "DailyResetWork"
    const val MIDNIGHT_HOUR = 0
    const val MIDNIGHT_MINUTE = 0
    const val MIDNIGHT_SECOND = 0
    const val MIDNIGHT_MILLISECOND = 0

    // Log tags
    const val TAG_MAIN_ACTIVITY = "MainActivity"
    const val TAG_RESET_WORKER = "ResetCheckmarksWorker"
    const val TAG_SCHEDULER = "DailyResetScheduler"
}

fun List<Task>.toJson(): String? {
    return try {
        Json.encodeToString(this)
    } catch (e: SerializationException) {
        Log.e("TaskSerialization", "Failed to serialize task list", e)
        null
    }
}

fun String.toTaskList(): List<Task>? {
    return try {
        Json.decodeFromString<List<Task>>(this)
    } catch (e: SerializationException) {
        Log.e("TaskSerialization", "Failed to deserialize task list", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.e("TaskSerialization", "Invalid JSON format", e)
        null
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContent {
                DailyChecklistTheme {
                    MainScreen(viewModel)
                }
            }
            scheduleDailyReset(applicationContext)
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_MAIN_ACTIVITY, "Error during onCreate", e)
            // Could show error dialog or fallback UI here
        }
    }
}



class ResetCheckmarksWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            val success = resetTaskCompletionStatus()
            if (success) {
                scheduleDailyReset(applicationContext)
                Result.success()
            } else {
                Log.w(AppConstants.TAG_RESET_WORKER, "Failed to reset tasks, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_RESET_WORKER, "Unexpected error during work", e)
            Result.failure()
        }
    }

    private fun resetTaskCompletionStatus(): Boolean {
        val sharedPreferences = applicationContext.getSharedPreferences(
            AppConstants.SHARED_PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return try {
            val jsonString = sharedPreferences.getString(AppConstants.TASK_LIST_KEY, null)

            if (jsonString == null) {
                Log.i(AppConstants.TAG_RESET_WORKER, "No tasks found to reset")
                return true // Not an error, just no tasks
            }

            val tasks = jsonString.toTaskList()
            if (tasks == null) {
                Log.e(AppConstants.TAG_RESET_WORKER, "Failed to parse tasks from storage")
                return false
            }

            val updatedTasks = tasks.map { it.copy(isCompleted = false) }
            val updatedJson = updatedTasks.toJson()

            if (updatedJson == null) {
                Log.e(AppConstants.TAG_RESET_WORKER, "Failed to serialize updated tasks")
                return false
            }

            // Store updated tasks back to SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putString(AppConstants.TASK_LIST_KEY, updatedJson)
            val commitSuccess = editor.commit() // Use commit() for synchronous operation in worker

            if (!commitSuccess) {
                Log.e(AppConstants.TAG_RESET_WORKER, "Failed to save updated tasks")
                return false
            }

            Log.i(AppConstants.TAG_RESET_WORKER, "Successfully reset ${updatedTasks.size} tasks")
            true

        } catch (e: Exception) {
            Log.e(AppConstants.TAG_RESET_WORKER, "Error accessing SharedPreferences", e)
            false
        }
    }
}

fun scheduleDailyReset(context: Context) {
    try {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, AppConstants.MIDNIGHT_HOUR)
            set(Calendar.MINUTE, AppConstants.MIDNIGHT_MINUTE)
            set(Calendar.SECOND, AppConstants.MIDNIGHT_SECOND)
            set(Calendar.MILLISECOND, AppConstants.MIDNIGHT_MILLISECOND)

            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()

        if (delay < 0) {
            Log.w(AppConstants.TAG_SCHEDULER, "Calculated negative delay, scheduling for next day")
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<ResetCheckmarksWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(AppConstants.DAILY_RESET_WORK_NAME)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            AppConstants.DAILY_RESET_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.i(AppConstants.TAG_SCHEDULER, "Daily reset scheduled for ${Date(calendar.timeInMillis)}")

    } catch (e: Exception) {
        Log.e(AppConstants.TAG_SCHEDULER, "Failed to schedule daily reset", e)
        // Could implement exponential backoff retry here
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    // Use mock data instead of real Application instance
    val mockViewModel = TaskViewModel(Application()).apply {
        // You could initialize with sample data here for preview
    }

    DailyChecklistTheme {
        MainScreen(mockViewModel)
    }
}