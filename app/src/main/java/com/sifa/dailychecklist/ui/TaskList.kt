package com.sifa.dailychecklist.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sifa.dailychecklist.model.Task

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    onTaskToggle: (Int) -> Unit,
    onTaskPriorityToggle: (Int) -> Unit,
    onTaskEdit: (Task) -> Unit,
    onTaskDelete: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = tasks,
            key = { it.id }
        ) { task ->
            TaskItem(
                task = task,
                onToggle = { onTaskToggle(task.id) },
                onPriorityToggle = { onTaskPriorityToggle(task.id) },
                onEdit = { onTaskEdit(task) },
                onDelete = { onTaskDelete(task.id) },
                modifier = Modifier.animateItemPlacement(
                    animationSpec = tween(300)
                )
            )
        }

        // Add bottom padding for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}