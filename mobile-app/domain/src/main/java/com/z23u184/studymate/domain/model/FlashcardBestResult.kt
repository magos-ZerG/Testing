package com.z23u184.studymate.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class FlashcardBestResult(
    val id: String,
    val topicId: TopicId,
    val questionsCount: Int,
    val durationMs: Long,
    val completedAt: Instant,
    val updatedAt: Instant,
)
