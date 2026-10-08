package com.z23u184.studymate.app.ui.task.tasklist

import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.domain.model.TaskStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class TopicTaskListScreenUiState(
    val topicId: String = "",
    val topicTitle: String = "",
    val isLoading: Boolean = true,
    val tasks: List<TaskListItemUi> = emptyList(),
    val errorMessage: UiText? = null,
) {
    val isEmpty: Boolean
        get() = tasks.isEmpty()
}

data class TaskListItemUi(
    val id: String,
    val title: String,
    val status: TaskStatus,
    @OptIn(ExperimentalTime::class)
    val deadlineAt: Instant? = null,
    val hasSolution: Boolean = false,
    val updatedAtText: String? = null,
)
