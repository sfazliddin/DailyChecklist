package com.sifa.dailychecklist

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sifa.dailychecklist.ui.theme.DailyChecklistTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
data class Task(
    val id: Int,
    val name: String,
    val isCompleted: Boolean
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val _tasks = MutableLiveData<MutableList<Task>>()
    val tasks: LiveData<MutableList<Task>> get() = _tasks

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val taskList = retrieveList(getApplication(), "myTaskList").toMutableList()
            withContext(Dispatchers.Main) {
                _tasks.value = taskList
            }
        }
    }

    fun addTask(task: Task) {
        val updatedTasks = _tasks.value ?: mutableListOf()
        updatedTasks.add(task)
        _tasks.value = updatedTasks
        storeList(getApplication(), "myTaskList", updatedTasks)
    }

    fun updateTasks(tasks: List<Task>) {
        _tasks.value = tasks.toMutableList()
        storeList(getApplication(), "myTaskList", tasks)
    }

    fun deleteTask(task: Task) {
        val updatedTasks = _tasks.value?.toMutableList() ?: mutableListOf()
        updatedTasks.remove(task)
        _tasks.value = updatedTasks
        storeList(getApplication(), "myTaskList", updatedTasks)
    }

    private fun storeList(context: Context, key: String, list: List<Task>) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        val jsonString = list.toJson()
        editor.putString(key, jsonString)
        editor.apply()
    }

    private fun retrieveList(context: Context, key: String): List<Task> {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(key, null)
        return jsonString?.toTaskList() ?: emptyList()
    }
}

fun List<Task>.toJson(): String = Json.encodeToString(this)
fun String.toTaskList(): List<Task> = Json.decodeFromString(this)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyChecklistTheme {
                val viewModel = ViewModelProvider(this)[TaskViewModel::class.java]
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.observeAsState(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    val sdfDate = SimpleDateFormat(
        "MMM d yyyy",
        Locale.getDefault()
    )
    val currentDate = sdfDate.format(Date())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Daily Checklist") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE), // Background color
                    titleContentColor = Color.White    // Text color
                ),

            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF6200EE),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        },
        bottomBar = {
            BottomAppBar (
                content = {Text(text=currentDate, fontSize = 36.sp, color = Color.White, textAlign = TextAlign.Center)},
                containerColor = Color(0xFF6200EE),
                windowInsets = WindowInsets(100.dp, 0.dp, 50.dp, 0.dp)
            )
        }
    ) { innerPadding ->
        TaskList(tasks, Modifier.padding(innerPadding), onTaskChange = { updatedTasks ->
            viewModel.updateTasks(updatedTasks)
        }, onEditTask = { task ->
            taskToEdit = task
            newTaskText = task.name
            showDialog = true
        })

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (taskToEdit != null) "Edit Task" else "Add New Task") },
                text = {
                    Column {
                        BasicTextField(
                            value = newTaskText,
                            onValueChange = { newTaskText = it },
                            singleLine = true,
                            textStyle = TextStyle(color= Color(0xFF30D5C8 )),
                            decorationBox = {innerTextField ->
                                Row (
                                    Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF6200EE), CircleShape)
                                        .height(18.dp)
                                        .padding(start = 8.dp)
                                ){
                                    if(newTaskText.isEmpty()){
                                        Text(text = "Enter Task Name", color = Color.Gray)
                                    }
                                    innerTextField()
                                }
                            },

                            )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTaskText.isNotBlank()) {
                                if (taskToEdit != null) {
                                    val updatedTask = taskToEdit!!.copy(name = newTaskText)
                                    val updatedTasks = tasks.toMutableList().apply {
                                        this[indexOf(taskToEdit!!)] = updatedTask
                                    }
                                    viewModel.updateTasks(updatedTasks)
                                } else {
                                    viewModel.addTask(Task(tasks.size, newTaskText, false))
                                }
                            }
                            newTaskText = ""
                            showDialog = false
                        }
                    ) {
                        Text(if (taskToEdit != null) "Update Task" else "Add Task")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDialog = false
                        newTaskText=""
                        taskToEdit=null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TaskList(tasks: List<Task>, modifier: Modifier = Modifier, onTaskChange: (List<Task>) -> Unit, onEditTask: (Task) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(tasks.size) { index ->
            TaskItem(
                task = tasks[index],
                onCheckedChange = { isChecked ->
                    val updatedTasks = tasks.toMutableList()
                    updatedTasks[index] = updatedTasks[index].copy(isCompleted = isChecked)
                    onTaskChange(updatedTasks)
                },
                onDelete = {
                    val updatedTasks = tasks.toMutableList()
                    updatedTasks.removeAt(index)
                    onTaskChange(updatedTasks)
                },
                onEditClick = { onEditTask(tasks[index]) }
            )
        }
    }
}

@Composable
fun TaskItem(task: Task, onCheckedChange: (Boolean) -> Unit, onDelete: () -> Unit, onEditClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = task.name)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Task")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Clear, contentDescription = "Delete Task")
        }
    }
}

class ResetCheckmarksWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("myTaskList", null)
        val tasks = jsonString?.toTaskList() ?: emptyList()
        val updatedTasks = tasks.map { it.copy(isCompleted = false) }

        // Store updated tasks back to SharedPreferences
        with(sharedPreferences.edit()) {
            putString("myTaskList", updatedTasks.toJson())
            apply()
        }

        return Result.success()
    }
}

fun scheduleDailyReset(context: Context) {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

    val workRequest = PeriodicWorkRequestBuilder<ResetCheckmarksWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "DailyResetWork",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val viewModel = TaskViewModel(Application()) // Mock ViewModel for preview
    DailyChecklistTheme {
        MainScreen(viewModel)
    }
}