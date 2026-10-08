package com.z23u184.studymate.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class TaskSolution(
    val id: TaskSolutionId,
    val taskId: TaskId,
    val content: String,
    val updatedAt: Instant
)
