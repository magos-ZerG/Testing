package com.z23u184.studymate.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class StudyTask(
    val id: TaskId,
    val topicId: TopicId,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deadlineAt: Instant?
)
