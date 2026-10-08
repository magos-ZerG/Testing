package com.z23u184.studymate.data.lab

import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.model.TopicId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
internal val LAB_FIXED_TIME: Instant = Instant.parse("2026-09-24T12:00:00Z")

internal data class CreateTaskInput(
    val topicId: TopicId,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val deadlineAt: Instant?,
)


@OptIn(ExperimentalTime::class)
internal class CreateTaskInputBuilder {
    private var topicId: TopicId = TopicId("topic-1")
    private var title: String = "Read chapter 1"
    private var description: String = "Prepare a short summary"
    private var status: TaskStatus = TaskStatus.PLANNED
    private var deadlineAt: Instant? = null

    fun withTopicId(value: TopicId) = apply { topicId = value }
    fun withTitle(value: String) = apply { title = value }
    fun withDescription(value: String) = apply { description = value }
    fun withStatus(value: TaskStatus) = apply { status = value }
    fun withDeadline(value: Instant?) = apply { deadlineAt = value }

    fun build() = CreateTaskInput(
        topicId = topicId,
        title = title,
        description = description,
        status = status,
        deadlineAt = deadlineAt,
    )
}

@OptIn(ExperimentalTime::class)
internal object TopicMother {
    fun existing(
        id: TopicId = TopicId("topic-1"),
        title: String = "Software testing",
    ) = Topic(
        id = id,
        title = title,
        createdAt = LAB_FIXED_TIME,
        updatedAt = LAB_FIXED_TIME,
    )
}
