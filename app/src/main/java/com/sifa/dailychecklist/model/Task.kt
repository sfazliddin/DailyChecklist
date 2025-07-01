package com.sifa.dailychecklist.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Int,
    val name: String,
    val isCompleted: Boolean,
    val isPriority: Boolean=false
)