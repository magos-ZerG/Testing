package com.z23u184.studymate.domain.model

data class Flashcard(
    val taskId: TaskId,
    val question: String,
    val answer: String,
)
