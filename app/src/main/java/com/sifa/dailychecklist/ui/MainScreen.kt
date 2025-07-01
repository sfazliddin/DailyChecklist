package com.sifa.dailychecklist.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sifa.dailychecklist.model.Task
import com.sifa.dailychecklist.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Material 3 Expressive Color Scheme
object AppColors {
    val Primary = Color(0xFF6750A4)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEADDFF)
    val OnPrimaryContainer = Color(0xFF21005D)
    val Secondary = Color(0xFF625B71)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE8DEF8)
    val OnSecondaryContainer = Color(0xFF1D192B)
    val Tertiary = Color(0xFF7D5260)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFFFD8E4)
    val OnTertiaryContainer = Color(0xFF31111D)
    val Success = Color(0xFF2E7D32)
    val Warning = Color(0xFFED6C02)
    val Error = Color(0xFFD32F2F)
    val Surface = Color(0xFFFEF7FF)
    val OnSurface = Color(0xFF1C1B1F)
    val SurfaceVariant = Color(0xFFE7E0EC)
    val OnSurfaceVariant = Color(0xFF49454F)
    val Outline = Color(0xFF79747E)
    val OutlineVariant = Color(0xFFCAC4D0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()

    var showDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    // Date formatting with proper locale handling
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val currentDate = remember { dateFormatter.format(Date()) }

    // Sort tasks: Priority tasks first, then by completion status, then by creation order
//    val sortedTasks = remember(tasks) {
//        tasks.sortedWith(compareBy<Task> { !it.isPriority } // Priority tasks first (false comes before true)
//            .thenBy { it.isCompleted } // Incomplete tasks before completed
//            .thenBy { it.id }) // Then by creation order (assuming lower ID = created earlier)
//    }
    val sortedTasks = remember(tasks) {
        val priorityTasks = tasks.filter { it.isPriority && !it.isCompleted }
            .sortedBy { it.id }
        val normalTasks = tasks.filter { !it.isPriority && !it.isCompleted }
            .sortedBy { it.id }
        val completedTasks = tasks.filter { it.isCompleted }
            .sortedWith(compareBy<Task> { !it.isPriority }.thenBy { it.id })

        priorityTasks + normalTasks + completedTasks
    }

    // Task statistics
    val completedTasks = remember(tasks) { tasks.count { it.isCompleted } }
    val totalTasks = remember(tasks) { tasks.size }
    val progressPercentage = remember(completedTasks, totalTasks) {
        if (totalTasks > 0) (completedTasks.toFloat() / totalTasks) else 0f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBarSection(
                    currentDate = currentDate,
                    completedTasks = completedTasks,
                    totalTasks = totalTasks,
                    progressPercentage = progressPercentage
                )
            },
            floatingActionButton = {
                EnhancedFAB(
                    onClick = {
                        taskToEdit = null
                        showDialog = true
                    }
                )
            },
            containerColor = AppColors.Surface
        ) { innerPadding ->

            when {
                isLoading -> {
                    LoadingScreen(modifier = Modifier.padding(innerPadding))
                }

                error != null -> {
                    ErrorScreen(
                        error = error ?: "Unknown error occurred",
                        onRetry = { viewModel.refreshTasks() },
                        onDismiss = { viewModel.clearError() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                tasks.isEmpty() -> {
                    EmptyTasksScreen(
                        onAddTask = { showDialog = true },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                else -> {
                    TaskList(
                        tasks = sortedTasks,
                        modifier = Modifier.padding(innerPadding),
                        onTaskToggle = { taskId -> viewModel.toggleTaskCompletion(taskId) },
                        onTaskPriorityToggle = { taskId -> viewModel.toggleTaskPriority(taskId) },
                        onTaskEdit = { task ->
                            taskToEdit = task
                            showDialog = true
                        },
                        onTaskDelete = { taskId -> viewModel.deleteTask(taskId) }
                    )
                }
            }
        }

        if (showDialog) {
            EnhancedTaskDialog(
                taskToEdit = taskToEdit,
                onDismiss = {
                    taskToEdit = null
                    showDialog = false
                },
                onConfirm = { taskName, isPriority ->
                    if (taskToEdit != null) {
                        val updatedTask = taskToEdit!!.copy(
                            name = taskName,
                            isPriority = isPriority
                        )
                        viewModel.updateTask(updatedTask)
                    } else {
                        val newTask = Task(
                            id = System.currentTimeMillis().toInt(), // Better ID generation
                            name = taskName,
                            isCompleted = false,
                            isPriority = isPriority
                        )
                        viewModel.addTask(newTask)
                    }
                    taskToEdit = null
                    showDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarSection(
    currentDate: String,
    completedTasks: Int,
    totalTasks: Int,
    progressPercentage: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 48.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.PrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daily Checklist",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnPrimaryContainer
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentDate,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = AppColors.OnPrimaryContainer.copy(alpha = 0.7f)
                )
            )

            if (totalTasks > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                ProgressIndicatorWithStats(
                    progress = progressPercentage,
                    completedTasks = completedTasks,
                    totalTasks = totalTasks
                )
            }
        }
    }
}

@Composable
private fun ProgressIndicatorWithStats(
    progress: Float,
    completedTasks: Int,
    totalTasks: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(80.dp),
                color = AppColors.OutlineVariant,
                strokeWidth = 8.dp,
                trackColor = Color.Transparent
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(80.dp),
                color = AppColors.Primary,
                strokeWidth = 8.dp,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnPrimaryContainer
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$completedTasks of $totalTasks tasks completed",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AppColors.OnPrimaryContainer.copy(alpha = 0.8f)
            )
        )
    }
}

@Composable
private fun EnhancedFAB(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    ExtendedFloatingActionButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        icon = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add Task",
                tint = AppColors.OnPrimary
            )
        },
        text = {
            Text(
                "Add Task",
                color = AppColors.OnPrimary,
                fontWeight = FontWeight.Medium
            )
        },
        containerColor = AppColors.Primary,
        modifier = Modifier
            .padding(16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp)
    )
}


@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.Primary,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading your tasks...",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = AppColors.OnSurfaceVariant
            )
        )
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = AppColors.Error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = AppColors.OnSurface,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AppColors.OnSurfaceVariant
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Dismiss")
            }
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun EmptyTasksScreen(
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Ready to get organized?",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = AppColors.OnSurface,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your first task to get started with your daily checklist",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = AppColors.OnSurfaceVariant
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddTask,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary
            ),
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Add Your First Task",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun EnhancedTaskDialog(
    taskToEdit: Task?,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var taskName by remember(taskToEdit) { mutableStateOf(taskToEdit?.name ?: "") }
    var isPriority by remember(taskToEdit) { mutableStateOf(taskToEdit?.isPriority ?: false) }
    val focusRequester = remember { FocusRequester() }
    val isEditing = taskToEdit != null

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Task" else "Add New Task",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task name") },
                    placeholder = { Text("Enter your task...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (taskName.isNotBlank()) {
                                onConfirm(taskName.trim(), isPriority)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary
                    )
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPriority) {
                            AppColors.TertiaryContainer
                        } else AppColors.SurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPriority = !isPriority }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isPriority) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (isPriority) AppColors.Tertiary else AppColors.OnSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Priority Task",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = if (isPriority) {
                                        AppColors.OnTertiaryContainer
                                    } else AppColors.OnSurfaceVariant
                                )
                            )
                            Text(
                                text = "Mark as high priority",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isPriority) {
                                        AppColors.OnTertiaryContainer.copy(alpha = 0.7f)
                                    } else AppColors.OnSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        }
                        Switch(
                            checked = isPriority,
                            onCheckedChange = { isPriority = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColors.Tertiary,
                                checkedTrackColor = AppColors.TertiaryContainer
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskName.isNotBlank()) {
                        onConfirm(taskName.trim(), isPriority)
                    }
                },
                enabled = taskName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isEditing) "Update Task" else "Add Task",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Cancel",
                    color = AppColors.OnSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = AppColors.Surface
    )
}