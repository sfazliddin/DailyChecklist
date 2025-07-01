package com.sifa.dailychecklist.data

import com.sifa.dailychecklist.model.Task

class SampleData {
    object SampleData {

        // Basic daily routine tasks
        val basicTasks = listOf(
            Task(
                id = 1,
                name = "Drink 8 glasses of water",
                isCompleted = false,
                isPriority = true
            ),
            Task(
                id = 2,
                name = "Exercise for 30 minutes",
                isCompleted = true,
                isPriority = true
            ),
            Task(
                id = 3,
                name = "Read for 20 minutes",
                isCompleted = false,
                isPriority = false
            ),
            Task(
                id = 4,
                name = "Take vitamins",
                isCompleted = true,
                isPriority = false
            ),
            Task(
                id = 5,
                name = "Plan tomorrow's tasks",
                isCompleted = false,
                isPriority = false
            )
        )

        // Work-focused tasks
        val workTasks = listOf(
            Task(
                id = 6,
                name = "Check and respond to emails",
                isCompleted = false,
                isPriority = true
            ),
            Task(
                id = 7,
                name = "Complete project review",
                isCompleted = false,
                isPriority = true
            ),
            Task(
                id = 8,
                name = "Team standup meeting",
                isCompleted = true,
                isPriority = false
            ),
            Task(
                id = 9,
                name = "Update project timeline",
                isCompleted = false,
                isPriority = false
            ),
            Task(
                id = 10,
                name = "Backup important files",
                isCompleted = false,
                isPriority = false
            )
        )

        // Personal care tasks
        val personalTasks = listOf(
            Task(
                id = 11,
                name = "Skincare routine",
                isCompleted = true,
                isPriority = false
            ),
            Task(
                id = 12,
                name = "Meditate for 10 minutes",
                isCompleted = false,
                isPriority = true
            ),
            Task(
                id = 13,
                name = "Call family/friends",
                isCompleted = false,
                isPriority = false
            ),
            Task(
                id = 14,
                name = "Tidy living space",
                isCompleted = true,
                isPriority = false
            ),
            Task(
                id = 15,
                name = "Prepare healthy meals",
                isCompleted = false,
                isPriority = false
            )
        )

        // Combined sample for testing
        val allSampleTasks = basicTasks + workTasks + personalTasks

        // Mixed completion status for realistic testing
        val mixedCompletionTasks = listOf(
            Task(1, "Morning coffee", true, false),
            Task(2, "Check weather", true, false),
            Task(3, "Review daily goals", false, true),
            Task(4, "Lunch break", false, false),
            Task(5, "Evening reflection", false, false),
            Task(6, "Stretch exercises", true, false),
            Task(7, "Water plants", false, false),
            Task(8, "Listen to podcast", false, false)
        )
    }

}