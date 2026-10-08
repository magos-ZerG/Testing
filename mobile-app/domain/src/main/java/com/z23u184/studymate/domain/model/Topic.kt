package com.z23u184.studymate.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class Topic(
    val id: TopicId,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
